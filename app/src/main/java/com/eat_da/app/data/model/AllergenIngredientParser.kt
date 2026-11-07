package com.eatda.app.data.model

import com.eatda.app.data.ALLERGEN_PRESETS

/**
 * OCR로 읽은 성분표에서 직접 함유와 같은 시설 제조 표시를 분리해 찾습니다.
 */
data class AllergenIngredientHit(
    val allergenName: String,
    val matchedTerms: List<String>,
)

data class AllergenIngredientResult(
    val directHits: List<AllergenIngredientHit>,
    val facilityHits: List<AllergenIngredientHit>,
    val rawText: String,
)

object AllergenIngredientParser {

    // ── 같은 시설 제조 문구 ──────────────────────────────

    private val facilityClausePatterns = listOf(
        Regex(
            "(?:본\\s*제품은|이\\s*제품은)\\s*.{0,1000}?" +
                    "(?:사용하는|사용한|취급하는)\\s*제품과\\s*" +
                    "(?:같은|동일한)\\s*(?:제조\\s*)?" +
                    "(?:시설|공장|생산\\s*라인|제조\\s*라인)\\s*" +
                    "(?:에서)?\\s*(?:제조|생산)" +
                    "(?:하고\\s*있습니다|됩니다|합니다)?",
            setOf(RegexOption.DOT_MATCHES_ALL),
        ),
        Regex(
            "(?:본\\s*제품은|이\\s*제품은)[^.!?。]{0,1000}?" +
                    "(?:같은|동일한)\\s*(?:제조\\s*)?" +
                    "(?:시설|공장)\\s*에서\\s*(?:제조|생산)" +
                    "(?:하고\\s*있습니다|됩니다|합니다)?",
            setOf(RegexOption.DOT_MATCHES_ALL),
        ),
        // "본 제품은" 없이 쓰인 문구
        // 예: "같은 제조시설에서 땅콩을 사용한 제품을 제조하고 있습니다"
        // 원재료가 같은 시설 표시로 잘못 분류되지 않도록 문장 구분 기호·줄바꿈·"함유"에서 멈춤
        Regex(
            "(?:(?!함유)[^.!?。\\n]){0,150}" +
                    "(?:같은|동일한)\\s*(?:제조\\s*)?" +
                    "(?:시설|공장|생산\\s*라인|제조\\s*라인)" +
                    "(?:(?!함유)[^.!?。\\n]){0,150}",
        ),
    )

    // ── 알레르기 원재료 별칭 ─────────────────────────────

    private val ingredientAliases = mapOf(
        "우유" to listOf(
            "전지분유", "탈지분유", "분유", "유청", "유청분말",
            "유청단백", "유당", "유단백", "카제인", "카제인나트륨",
            "유크림", "크림분말", "버터분말",
        ),
        "알류" to listOf(
            "난류", "난백", "난황", "전란액", "난백액",
            "난황액", "전란분", "난백분", "계란분말",
            "달걀분말", "난각분말", "계란", "달걀",
        ),
        "밀" to listOf(
            "밀가루", "밀전분", "밀단백", "밀글루텐", "소맥", "소맥분",
        ),
        "대두" to listOf(
            "대두단백", "분리대두단백", "대두분말",
        ),
        "게" to listOf(
            "게살", "게추출물", "게엑기스",
        ),
        "조개류" to listOf(
            "굴소스", "굴추출물", "조개추출물", "바지락추출물",
        ),
        "잣" to listOf(
            "잣가루",
        ),
    )

    // 직접 입력한 이름을 대표 알레르기 항목으로 연결
    // 예: 기본값 "계란"으로 등록해도 성분표의 "난백분"·"난류"까지 찾음
    private val allergenGroups = mapOf(
        "계란" to "알류",
        "달걀" to "알류",
        "난류" to "알류",
        "소고기" to "쇠고기",
        "조개" to "조개류",
        "굴" to "조개류",
        "콩" to "대두",
    )

    // 다른 단어에 섞이기 쉬운 한 글자 표기
    // 부분 문자열로 찾으면 오검출(메밀→밀, ~하게→게)이 나므로 단어 경계로 따로 검사
    private val singleCharTerms = mapOf(
        "밀" to "밀",
        "게" to "게",
        "잣" to "잣",
        "조개류" to "굴",
    )

    // ── 외부 분석 함수 ───────────────────────────────────

    fun analyze(
        text: String,
        registeredAllergens: List<Allergen>,
    ): AllergenIngredientResult {
        val facilityRanges = findFacilityRanges(text)

        val facilityText = facilityRanges.joinToString(" ") { range ->
            text.substring(range)
        }

        // 시설 제조 경고 문구만 제거하고 나머지 원문은 유지합니다.
        val directText = buildString {
            var cursor = 0

            facilityRanges.forEach { range ->
                if (cursor < range.first) {
                    append(text.substring(cursor, range.first))
                    append(' ')
                }

                cursor = maxOf(cursor, range.last + 1)
            }

            if (cursor < text.length) {
                append(text.substring(cursor))
            }
        }

        return AllergenIngredientResult(
            directHits = findHits(directText, registeredAllergens),
            facilityHits = findHits(facilityText, registeredAllergens),
            rawText = text,
        )
    }

    // ── 시설 제조 문구 추출 ──────────────────────────────

    private fun findFacilityRanges(text: String): List<IntRange> {
        val candidates = facilityClausePatterns
            .flatMap { pattern ->
                pattern.findAll(text).map { it.range }.toList()
            }
            .filter { !it.isEmpty() }
            .sortedBy { it.first }

        if (candidates.isEmpty()) return emptyList()

        // 중복되거나 겹치는 범위를 하나로 합칩니다.
        val mergedRanges = mutableListOf<IntRange>()

        candidates.forEach { candidate ->
            val lastRange = mergedRanges.lastOrNull()

            if (lastRange == null || candidate.first > lastRange.last + 1) {
                mergedRanges.add(candidate)
            } else {
                mergedRanges[mergedRanges.lastIndex] =
                    lastRange.first..maxOf(lastRange.last, candidate.last)
            }
        }

        return mergedRanges
    }

    // ── 알레르기 성분 검색 ───────────────────────────────

    private fun findHits(
        text: String,
        allergens: List<Allergen>,
    ): List<AllergenIngredientHit> {
        val normalizedText = normalize(text)

        if (normalizedText.isBlank()) return emptyList()

        return allergens.mapNotNull { allergen ->
            // 직접 입력한 이름("계란")도 대표 항목("알류") 기준으로 별칭을 가져옴
            val groupName = allergenGroups[allergen.name] ?: allergen.name

            val preset = ALLERGEN_PRESETS.firstOrNull {
                it.name == groupName
            }

            val aliases = buildList {
                // 대표 알레르기 항목으로 등록된 경우 프리셋 별칭을 추가합니다.
                addAll(preset?.aliases.orEmpty())
                addAll(ingredientAliases[groupName].orEmpty())
                add(groupName)
                add(allergen.name)
            }.distinct()

            val matchedTerms = aliases
                .filter { alias ->
                    val normalizedAlias = normalize(alias)
                    normalizedAlias.length >= 2 &&
                            containsAlias(normalizedText, normalizedAlias)
                }
                .sortedByDescending { it.length }
                .distinct()

            // 한 글자 표기("밀", "게", "잣", "굴")는 단어 경계로 따로 검사합니다.
            val singleCharEvidence = buildList {
                singleCharTerms[groupName]?.let { add(it) }
                if (allergen.name.length == 1) add(allergen.name)
            }
                .distinct()
                .filter { term -> isExplicitSingleCharMention(text, term) }

            // 전지분유가 발견되면 더 짧은 분유는 중복 근거에서 제외합니다.
            val specificMatches = matchedTerms.filter { candidate ->
                matchedTerms.none { other ->
                    other != candidate &&
                            normalize(other).contains(normalize(candidate))
                }
            }

            (specificMatches + singleCharEvidence)
                .distinct()
                .takeIf { it.isNotEmpty() }
                ?.let { matches ->
                    AllergenIngredientHit(
                        allergenName = allergen.name,
                        matchedTerms = matches,
                    )
                }
        }
    }

    // "밀가루"처럼 밀로 시작하는 별칭은 "메밀가루"에 들어 있어도 밀로 보지 않습니다.
    private fun containsAlias(normalizedText: String, normalizedAlias: String): Boolean =
        if (normalizedAlias.startsWith("밀")) {
            Regex("(?<!메)" + Regex.escape(normalizedAlias)).containsMatchIn(normalizedText)
        } else {
            normalizedAlias in normalizedText
        }

    // ── 한 글자 표기 별도 검사 ────────────────────────────

    // 예: "대두, 밀, 게 함유" / "잣 함유" → 검출
    //     "메밀", "~하게", "굴비" → 검출 안 함
    private fun isExplicitSingleCharMention(text: String, term: String): Boolean {
        val t = Regex.escape(term)

        val patterns = listOf(
            // 앞뒤가 한글이 아닌 단독 표기: "밀,", "(게)", "잣 "
            Regex("(?:^|[^가-힣])$t(?:$|[^가-힣])"),
            // "밀 함유", "게함유"처럼 바로 뒤에 함유 표시가 붙은 경우
            Regex("(?<![가-힣])$t\\s*(?:함유|포함|사용|첨가|원료)"),
        )

        return patterns.any { pattern ->
            pattern.containsMatchIn(text)
        }
    }

    // ── 텍스트 정규화 ─────────────────────────────────────

    private fun normalize(value: String): String {
        return value
            .lowercase()
            .replace(
                Regex("[\\s·ㆍ,，:：()（）\\[\\]{}<>〈〉『』「」\\\"'’‘]"),
                "",
            )
    }
}
