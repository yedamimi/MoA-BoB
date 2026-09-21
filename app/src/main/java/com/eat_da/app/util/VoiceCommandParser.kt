package com.eatda.app.util

/** 음성 인식 결과를 앱 명령으로 파싱 */
sealed class VoiceCommand {
    /** "우유 유통기한 언제야?" */
    data class ExpiryQuery(val foodName: String) : VoiceCommand()
    /** "냉장고에 우유 있어?" */
    data class SearchFood(val foodName: String)  : VoiceCommand()
    /** "유통기한 하루 남은 거 있어?" — days=1,2,3,7 등 */
    data class ExpiringSoon(val days: Int = 3)   : VoiceCommand()
    /** "우유 삭제해줘" */
    data class DeleteFood(
        val foodName: String,
        val qty: Int? = null
    ) : VoiceCommand()
    /** "재고 추가 모드 열어줘" → 추가 모드 진입 */
    object EnterAddMode                          : VoiceCommand()
    /** 추가 모드에서 "끝" / "이전으로" → 추가 모드 종료 */
    object ExitAddMode                           : VoiceCommand()
    /** 추가 모드에서 "우유 1개 12월 5일" / "사과 3개" */
    data class AddFood(
        val name:   String,
        val qty:    String,
        val expiry: String?,  // "YYYY-MM-DD" or null → 식약처 기본값 사용
    ) : VoiceCommand()
    /** 추가 모드에서 여러 식품을 한 번에 — "사과 1개 우유 1개 당근 2개" */
    data class AddFoods(val items: List<AddFood>) : VoiceCommand()
    /** "응", "네" → 삭제 확인 */
    object Confirm                               : VoiceCommand()
    /** "아니", "취소" → 삭제 취소 */
    object Cancel                                : VoiceCommand()
    /** "끝" / "이전" / "이전으로" → 오버레이 닫기 (일반 모드 전용) */
    object Back                                  : VoiceCommand()
    /** 인식 불가 */
    data class Unknown(val text: String)         : VoiceCommand()
}

object VoiceCommandParser {

    private val CONFIRM = setOf("응", "어", "네", "예", "맞아", "그래", "좋아", "ㅇ", "맞아요", "넹", "응응")
    private val CANCEL  = setOf("아니", "아니오", "취소", "싫어", "안 해", "안해", "그만", "됐어", "괜찮아")

    /** 추가 모드 종료 키워드 */
    private val EXIT_ADD = setOf("끝", "종료", "그만", "닫아", "끝내", "나가", "이전으로", "이전")

    /** 일반 모드 종료 키워드 */
    private val STOP_KEYWORDS = setOf(
        "끝", "종료", "그만", "닫아", "끝내", "나가", "그만해", "종료해", "닫기", "없애", "이전으로", "이전"
    )

    /** 텍스트에서 질문 키워드를 제거해 식품명 추출 — 재고에 없는 식품 질문 처리용 */
    private fun extractFoodName(text: String): String? {
        val removePatterns = listOf(
            "냉장고에서", "냉장고에", "냉장고", "냉동실에", "냉동실",
            "유통기한", "소비기한", "유효기간", "기한",
            "있어요", "있나요", "있어", "있나", "있니", "있냐", "있음",
            "언제야", "언제", "얼마나", "남았어", "남아", "남았",
            "삭제해줘", "삭제해", "삭제", "지워줘", "지워",
            "없애줘", "없애", "버려줘", "버려", "제거해줘", "제거",
            "알려줘", "알려", "해줘", "줘",
            "?", "?", "~", "!", ".", ",",
        )
        var result = text
        removePatterns.sortedByDescending { it.length }.forEach { result = result.replace(it, " ") }
        return result.trim().split(Regex("\\s+")).firstOrNull { it.isNotEmpty() }
    }

    /** 한글 수량 표현 → 숫자 변환 ("한개" → "1개") */
    private fun normalizeNumbers(text: String): String {
        val map = mapOf(
            "한" to "1", "두" to "2", "세" to "3", "네" to "4",
            "다섯" to "5", "여섯" to "6", "일곱" to "7", "여덟" to "8",
            "아홉" to "9", "열" to "10",
        )
        var result = text
        map.forEach { (kor, num) ->
            result = result.replace(Regex("${kor}\\s*개"), "${num}개")
        }
        return result
    }

    /**
     * @param isAddMode true이면 재고 추가 모드 — 식품명+수량+날짜 패턴 우선 파싱
     */
    fun parse(text: String, inventoryNames: List<String>, isAddMode: Boolean = false): VoiceCommand {
        val t = text.trim()

        // ── 확인 / 취소 ─────────────────────────────────────────────────────
        if (CONFIRM.any { it.equals(t, ignoreCase = true) }) return VoiceCommand.Confirm
        if (CANCEL.any  { t.startsWith(it, ignoreCase = true) }) return VoiceCommand.Cancel

        // ── 재고 추가 모드 처리 ──────────────────────────────────────────────
        if (isAddMode) {
            // 종료 ("끝", "이전으로" 등)
            if (EXIT_ADD.any { t.contains(it) }) return VoiceCommand.ExitAddMode

            val normalized  = normalizeNumbers(t)
            val itemPattern = Regex("""(.+?)\s+(\d+)\s*개""")
            val datePattern = Regex("""(?:(\d{2,4})년\s*)?(\d+)월\s*(\d+)일""")
            val matches     = itemPattern.findAll(normalized).toList()

            if (matches.isEmpty()) return VoiceCommand.Unknown(t)

            fun parseExpiry(dateMatch: MatchResult?): String? {
                dateMatch ?: return null
                val rawYear = dateMatch.groupValues[1]
                val year = when {
                    rawYear.isEmpty()   -> java.time.LocalDate.now().year.toString()
                    rawYear.length <= 2 -> "20$rawYear"
                    else                -> rawYear
                }
                val month = dateMatch.groupValues[2]
                val day   = dateMatch.groupValues[3]
                return if (month.isNotEmpty() && day.isNotEmpty())
                    "$year-${month.padStart(2, '0')}-${day.padStart(2, '0')}"
                else null
            }

            if (matches.size == 1) {
                val m      = matches[0]
                val name   = m.groupValues[1].trim()
                val qty    = "${m.groupValues[2]}개"
                val after  = normalized.substring(m.range.last + 1)
                val expiry = parseExpiry(datePattern.find(after))
                return VoiceCommand.AddFood(name, qty, expiry)
            }

            // 여러 식품 — "사과 1개 오렌지 1개" / "사과 1개 우유 1개 12월 5일"
            val items = matches.mapIndexed { i, m ->
                val name       = m.groupValues[1].trim()
                val qty        = "${m.groupValues[2]}개"
                val afterStart = m.range.last + 1
                val afterEnd   = if (i + 1 < matches.size) matches[i + 1].range.first else normalized.length
                val afterText  = normalized.substring(afterStart, afterEnd)
                val expiry     = parseExpiry(datePattern.find(afterText))
                VoiceCommand.AddFood(name, qty, expiry)
            }
            return VoiceCommand.AddFoods(items)
        }

        val normalizedText = normalizeNumbers(t)

        val deleteQty = Regex("""(\d+)\s*개""")
            .find(normalizedText)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()

        // ── 일반 모드 오버레이 닫기 ("끝", "이전", "이전으로" 등) ──────────────
        if (STOP_KEYWORDS.any { t.contains(it) }) return VoiceCommand.Back

        // ── 추가 모드 진입 ───────────────────────────────────────────────────
        val enterKeywords = listOf("재고 추가 모드", "추가 모드", "재고추가모드", "추가모드")
        if (enterKeywords.any { it in t }) return VoiceCommand.EnterAddMode

        // ── 임박 식품 조회 ───────────────────────────────────────────────────
        val daysFromText: Int = when {
            "하루" in t                               -> 1
            "이틀" in t                               -> 2
            "사흘" in t                               -> 3
            "나흘" in t                               -> 4
            "닷새" in t                               -> 5
            "일주일" in t                             -> 7
            "오늘" in t && "만료" in t                -> 1
            else -> {
                Regex("""(\d+)\s*일""").find(t)
                    ?.groupValues?.get(1)?.toIntOrNull() ?: 3
            }
        }
        val expiryNearKeywords = listOf("상하는", "임박", "곧 상", "유통기한 얼마", "만료")
        val foodContext        = listOf("음식", "식품", "식재료", "거", "것", "게")
        val isExpiringSoon = expiryNearKeywords.any { it in t }
                || ("곧" in t && foodContext.any { it in t })
                || ("얼마" in t && ("남았" in t || "안 남" in t))
                || ("유통기한" in t && "남은" in t)
                || ("유통기한" in t && foodContext.any { it in t } && ("있어" in t || "알려줘" in t))
        if (isExpiringSoon) return VoiceCommand.ExpiringSoon(daysFromText)

        // ── 재고에서 식품명 탐색 ─────────────────────────────────────────────
        val foodName = inventoryNames
            .filter { it in t }
            .maxByOrNull { it.length }

        val expiryWords = listOf("유통기한", "기한", "언제", "얼마나 남", "소비기한", "유효기간")
        val searchWords = listOf("있어", "있나", "있니", "있냐", "있나요", "있어요", "있음")
        val deleteWords = listOf("삭제", "지워", "없애", "버려", "삭제해", "지워줘", "없애줘", "제거")

        // ── 재고에 있는 경우 ─────────────────────────────────────────────────
        if (foodName != null) {
            if (expiryWords.any { it in t }) return VoiceCommand.ExpiryQuery(foodName)
            if (searchWords.any { it in t }) return VoiceCommand.SearchFood(foodName)
            if (deleteWords.any { it in t }) {
                return VoiceCommand.DeleteFood(foodName, deleteQty)
            }
        }

        // ── 재고에 없어도 질문 패턴이면 식품명 추출 후 응답 ─────────────────
        // ex) "냉장고에 계란 있어?" → SearchFood("계란") → "계란은 없습니다"
        val guessedName = extractFoodName(t)
        if (guessedName != null) {
            if (expiryWords.any { it in t }) return VoiceCommand.ExpiryQuery(guessedName)
            if (searchWords.any { it in t }) return VoiceCommand.SearchFood(guessedName)
            if (deleteWords.any { it in t }) {
                return VoiceCommand.DeleteFood(guessedName, deleteQty)
            }
        }

        return VoiceCommand.Unknown(t)
    }
}
