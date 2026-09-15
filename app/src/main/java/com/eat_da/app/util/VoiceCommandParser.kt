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
    data class DeleteFood(val foodName: String)  : VoiceCommand()
    /** "응", "네" → 삭제 확인 */
    object Confirm                               : VoiceCommand()
    /** "아니", "취소" → 삭제 취소 */
    object Cancel                                : VoiceCommand()
    /** 인식 불가 */
    data class Unknown(val text: String)         : VoiceCommand()
}

object VoiceCommandParser {

    private val CONFIRM = setOf("응", "어", "네", "예", "맞아", "그래", "좋아", "ㅇ", "맞아요", "넹", "응응")
    private val CANCEL  = setOf("아니", "아니오", "취소", "싫어", "안 해", "안해", "그만", "됐어", "괜찮아")

    fun parse(text: String, inventoryNames: List<String>): VoiceCommand {
        val t = text.trim()

        // ── 확인 / 취소 ─────────────────────────────────────────────────────
        if (CONFIRM.any { it.equals(t, ignoreCase = true) }) return VoiceCommand.Confirm
        if (CANCEL.any  { t.startsWith(it, ignoreCase = true) }) return VoiceCommand.Cancel

        // ── 임박 식품 조회 ───────────────────────────────────────────────────
        // "유통기한 하루 남은 거 있어?", "유통기한 5일 남은 거 있어?" 등
        val daysFromText: Int = when {
            "하루" in t                               -> 1
            "이틀" in t                               -> 2
            "사흘" in t                               -> 3
            "나흘" in t                               -> 4
            "닷새" in t                               -> 5
            "일주일" in t                             -> 7
            "오늘" in t && "만료" in t                -> 1
            else -> {
                // "5일", "10일" 등 숫자+일 패턴
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

        // ── 재고에서 식품명 탐색 (긴 이름 우선 — "방울토마토" > "토마토") ──
        val foodName = inventoryNames
            .filter { it in t }
            .maxByOrNull { it.length }

        // ── 유통기한 조회 ────────────────────────────────────────────────────
        val expiryWords = listOf("유통기한", "기한", "언제", "얼마나 남", "소비기한", "유효기간")
        if (foodName != null && expiryWords.any { it in t }) {
            return VoiceCommand.ExpiryQuery(foodName)
        }

        // ── 식품 검색 ────────────────────────────────────────────────────────
        val searchWords = listOf("있어", "있나", "있니", "있냐", "있나요", "있어요", "있음")
        if (foodName != null && searchWords.any { it in t }) {
            return VoiceCommand.SearchFood(foodName)
        }

        // ── 삭제 ─────────────────────────────────────────────────────────────
        val deleteWords = listOf("삭제", "지워", "없애", "버려", "삭제해", "지워줘", "없애줘", "제거")
        if (foodName != null && deleteWords.any { it in t }) {
            return VoiceCommand.DeleteFood(foodName)
        }

        return VoiceCommand.Unknown(t)
    }
}
