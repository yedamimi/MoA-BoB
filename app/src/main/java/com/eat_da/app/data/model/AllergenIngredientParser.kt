package com.eatda.app.data.model

import com.eatda.app.data.ALLERGEN_PRESETS

/** OCR로 읽은 성분표에서 직접 함유와 같은 시설 제조 표시를 분리해 찾습니다. */
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
    /*
     * 식품 포장지의 대표적인 주의 문구를 문장 단위로 추출합니다.
     * OCR이 줄바꿈을 넣거나 공백을 생략해도 인식할 수 있도록 공백을 허용합니다.
     * 핵심은 '본 제품은 ... 사용하는 제품과 같은 제조시설에서 제조...' 전체를
     * 시설 제조 문구로 분리해 직접 함유 검사 대상에서 제외하는 것입니다.
     */
    private val facilityClausePatterns = listOf(
        Regex(
            "(?:본\\s*제품은|이\\s*제품은)\\s*.{0,1000}?(?:사용하는|사용한|취급하는)\\s*제품과\\s*(?:같은|동일한)\\s*(?:제조\\s*)?(?:시설|공장|생산\\s*라인|제조\\s*라인)\\s*(?:에서)?\\s*(?:제조|생산)(?:하고\\s*있습니다|됩니다|합니다)?",
            setOf(RegexOption.DOT_MATCHES_ALL)
        ),
        // '같은 제조시설에서 제조하고 있습니다'처럼 앞부분이 일부 누락된 OCR의 보수적 fallback
        Regex(
            "(?:본\\s*제품은|이\\s*제품은)[^.!?。]{0,1000}?(?:같은|동일한)\\s*(?:제조\\s*)?(?:시설|공장)\\s*에서\\s*(?:제조|생산)(?:하고\\s*있습니다|됩니다|합니다)?",
            setOf(RegexOption.DOT_MATCHES_ALL)
        ),
    )

    // 성분표에 자주 등장하는 표기를 대표 알레르기 항목에 추가합니다.
    private val ingredientAliases = mapOf(
        "우유" to listOf(
            "전지분유", "탈지분유", "분유", "유청", "유청분말", "유청단백", "유당",
            "유단백", "카제인", "카제인나트륨", "유크림", "크림분말", "버터분말",
        ),
        "알류" to listOf(
            "난류", "난백", "난황", "전란액", "난백액", "난황액", "전란분", "난백분",
            "계란분말", "달걀분말", "난각분말",
        ),
        // '밀'은 한 글자라 일반 부분 문자열 검색에서 제외되므로 명시적 표기를 별도 등록합니다.
        "밀" to listOf("밀가루", "밀전분", "밀단백", "밀글루텐", "소맥", "소맥분"),
    )

    fun analyze(text: String, registeredAllergens: List<Allergen>): AllergenIngredientResult {
        val facilityRanges = findFacilityRanges(text)
        val facilityText = facilityRanges.joinToString(" ") { range -> text.substring(range) }

        // 시설 제조 경고 문구만 원문에서 제거합니다. 나머지 원재료명과 '대두, 밀 함유'는 유지됩니다.
        val directText = buildString {
            var cursor = 0
            facilityRanges.forEach { range ->
                if (cursor < range.first) append(text.substring(cursor, range.first)).append(' ')
                cursor = maxOf(cursor, range.last + 1)
            }
            if (cursor < text.length) append(text.substring(cursor))
        }

        val directHits = findHits(directText, registeredAllergens)
        val facilityHits = findHits(facilityText, registeredAllergens)

        return AllergenIngredientResult(
            directHits = directHits,
            facilityHits = facilityHits,
            rawText = text,
        )
    }

    private fun findFacilityRanges(text: String): List<IntRange> {
        val candidates = facilityClausePatterns
            .flatMap { pattern -> pattern.findAll(text).map { it.range }.toList() }
            .sortedBy { it.first }

        if (candidates.isEmpty()) return emptyList()

        // 여러 정규식이 동일 문구를 잡거나 겹치는 경우 하나의 범위로 합칩니다.
        val merged = mutableListOf<IntRange>()
        candidates.forEach { candidate ->
            val last = merged.lastOrNull()
            if (last == null || candidate.first > last.last + 1) {
                merged += candidate
            } else {
                merged[merged.lastIndex] = last.first..maxOf(last.last, candidate.last)
            }
        }
        return merged
    }

    private fun findHits(text: String, allergens: List<Allergen>): List<AllergenIngredientHit> {
        val normalizedText = normalize(text)
        if (normalizedText.isBlank()) return emptyList()

        return allergens.mapNotNull { allergen ->
            val preset = ALLERGEN_PRESETS.firstOrNull { it.name == allergen.name }
            val aliases = buildList {
                // 대표 칩으로 등록한 경우에만 해당 프리셋의 별칭을 확장합니다.
                addAll(preset?.aliases.orEmpty())
                addAll(ingredientAliases[allergen.name].orEmpty())
                add(allergen.name)
            }.distinct()

            val matched = aliases
                .filter { alias ->
                    val normalizedAlias = normalize(alias)
                    normalizedAlias.length >= 2 && normalizedAlias in normalizedText
                }
                .sortedByDescending(String::length)
                .distinct()

            // '밀' 한 글자 자체를 무조건 부분 문자열로 찾으면 '메밀'까지 밀로 오인하므로
            // '밀 함유'처럼 독립 표기이거나 밀가루·소맥 등 명확한 원료명일 때만 인정합니다.
            val wheatEvidence = if (allergen.name == "밀" && isExplicitWheatMention(text)) {
                listOf("밀")
            } else emptyList()
            // '전지분유'가 발견됐을 때 더 짧은 '분유'까지 중복 근거로 표시하지 않습니다.
            val specificMatches = matched.filter { candidate ->
                matched.none { other -> other != candidate && normalize(other).contains(normalize(candidate)) }
            }

            (specificMatches + wheatEvidence).distinct().takeIf { it.isNotEmpty() }?.let {
                AllergenIngredientHit(allergenName = allergen.name, matchedTerms = it)
            }
        }
    }

    private fun isExplicitWheatMention(text: String): Boolean {
        val compact = normalize(text)
        return listOf(
            Regex("(?:^|[^가-힣])밀(?:$|[^가-힣])"), // 구분 기호가 살아 있는 독립 표기
            Regex("(?<!메)밀(?:함유|포함|사용|첨가|원료)"),
            Regex("(?<!메)(?:밀가루|밀전분|밀단백|밀글루텐)"),
            Regex("(?:소맥분|소맥)"),
        ).any { it.containsMatchIn(compact) || it.containsMatchIn(text) }
    }

    private fun normalize(value: String): String = value
        .lowercase()
        .replace(Regex("[\\s·ㆍ,，:：()（）\\[\\]{}<>〈〉『』「」\\\"'’‘]"), "")
}
