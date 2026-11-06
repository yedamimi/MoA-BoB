
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
            "달걀분말", "난각분말",
        ),
        "밀" to listOf(
            "밀가루", "밀전분", "밀단백", "밀글루텐", "소맥", "소맥분",
        ),
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
            val preset = ALLERGEN_PRESETS.firstOrNull {
                it.name == allergen.name
            }

            val aliases = buildList {
                // 대표 알레르기 항목으로 등록된 경우 프리셋 별칭을 추가합니다.
                addAll(preset?.aliases.orEmpty())
                addAll(ingredientAliases[allergen.name].orEmpty())
                add(allergen.name)
            }.distinct()

            val matchedTerms = aliases
                .filter { alias ->
                    val normalizedAlias = normalize(alias)
                    normalizedAlias.length >= 2 &&
                            normalizedAlias in normalizedText
                }
                .sortedByDescending { it.length }
                .distinct()

            // 밀은 단독 부분 문자열 검색 시 메밀까지 오인식할 수 있어 별도로 검사합니다.
            val wheatEvidence = if (
                allergen.name == "밀" && isExplicitWheatMention(text)
            ) {
                listOf("밀")
            } else {
                emptyList()
            }

            // 전지분유가 발견되면 더 짧은 분유는 중복 근거에서 제외합니다.
            val specificMatches = matchedTerms.filter { candidate ->
                matchedTerms.none { other ->
                    other != candidate &&
                            normalize(other).contains(normalize(candidate))
                }
            }

            (specificMatches + wheatEvidence)
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

    // ── 밀 성분 별도 검사 ─────────────────────────────────

    private fun isExplicitWheatMention(text: String): Boolean {
        val normalizedText = normalize(text)

        val wheatPatterns = listOf(
            Regex("(?:^|[^가-힣])밀(?:$|[^가-힣])"),
            Regex("(?<!메)밀(?:함유|포함|사용|첨가|원료)"),
            Regex("(?<!메)(?:밀가루|밀전분|밀단백|밀글루텐)"),
            Regex("(?:소맥분|소맥)"),
        )

        return wheatPatterns.any { pattern ->
            pattern.containsMatchIn(normalizedText) ||
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
