package com.eatda.app.util

// 재고 수량. Firebase에는 qty(정수) + unit(문자열)으로 저장됨
data class Qty(val amount: Int, val unit: String)

private val QTY_REGEX = Regex("""^\s*(\d+)\s*([^\d/.]*)$""")

// "3개" → Qty(3, "개"), "500ml" → Qty(500, "ml"), "2" → Qty(2, defaultUnit)
// "1/2통", "0.5kg"처럼 정수가 아니거나 숫자가 없는 값은 null
fun parseQty(qty: String, defaultUnit: String = "개"): Qty? {
    val match = QTY_REGEX.find(qty) ?: return null
    val amount = match.groupValues[1].toIntOrNull() ?: return null
    val unit = match.groupValues[2].trim().ifEmpty { defaultUnit }
    return Qty(amount, unit)
}
