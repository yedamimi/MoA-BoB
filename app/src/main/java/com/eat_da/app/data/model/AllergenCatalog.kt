package com.eatda.app.data

// 마이페이지에서 바로 고를 수 있는 대표 알레르기 항목
// aliases: 이 알레르기로 경고할 식재료 이름 (재고·스캔 이름과 비교)
data class AllergenPreset(
    val name: String,
    val emoji: String,
    val aliases: List<String>,
)

// 식품 알레르기 표시 대상 품목 중 식재료 이름으로 판별할 수 있는 항목
val ALLERGEN_PRESETS: List<AllergenPreset> = listOf(
    AllergenPreset("우유", "🥛", listOf("우유", "치즈", "요거트", "요구르트", "버터", "생크림", "연유", "분유")),
    AllergenPreset("알류", "🥚", listOf("계란", "달걀", "메추리알", "오리알")),
    AllergenPreset("땅콩", "🥜", listOf("땅콩")),
    AllergenPreset("대두", "🫘", listOf("대두", "콩", "콩나물", "두부", "두유", "된장", "간장", "청국장", "쌈장")),
    AllergenPreset("밀", "🌾", listOf(
        "밀가루", "빵", "식빵", "빵가루", "국수", "칼국수", "라면", "파스타", "스파게티",
        "만두", "부침가루", "튀김가루", "칼빔면", "샌드위치", "피자", "케이크", "도넛", "핫도그",
    )),
    AllergenPreset("메밀", "🍜", listOf("메밀", "메밀국수", "메밀면")),
    AllergenPreset("새우", "🦐", listOf("새우", "새우깡", "새우젓")),
    AllergenPreset("게", "🦀", listOf("게", "꽃게", "대게", "킹크랩", "게맛살")),
    AllergenPreset("조개류", "🦪", listOf("조개", "굴", "전복", "홍합", "바지락", "가리비", "꼬막")),
    AllergenPreset("오징어", "🦑", listOf("오징어")),
    AllergenPreset("고등어", "🐟", listOf("고등어")),
    AllergenPreset("돼지고기", "🐷", listOf("돼지고기", "삼겹살", "목살", "앞다리살", "베이컨", "햄", "소시지", "핫도그")),
    AllergenPreset("쇠고기", "🐮", listOf("쇠고기", "소고기", "한우", "차돌박이")),
    AllergenPreset("닭고기", "🐔", listOf("닭고기", "닭", "닭가슴살", "닭다리", "치킨")),
    AllergenPreset("복숭아", "🍑", listOf("복숭아")),
    AllergenPreset("토마토", "🍅", listOf("토마토", "방울토마토", "케첩")),
    AllergenPreset("호두", "🌰", listOf("호두")),
    AllergenPreset("잣", "🌰", listOf("잣")),
)

fun allergenPreset(name: String): AllergenPreset? =
    ALLERGEN_PRESETS.firstOrNull { it.name == name }

// 등록한 알레르기(allergenName)로 이 식재료(foodName)를 경고해야 하는지
//
// - 대표 항목이면 aliases 전체, 직접 입력한 항목이면 그 이름으로 비교
// - 두 글자 이상은 포함 여부로 비교 ("국산콩 두유" ← 두유, "새우깡" ← 새우)
// - 한 글자("게", "굴", "콩" 등)는 다른 단어에 섞여 오경고가 나지 않도록 정확히 같을 때만
fun allergenMatches(allergenName: String, foodName: String): Boolean {
    val food = foodName.replace(" ", "")
    if (food.isEmpty()) return false
    val keywords = allergenPreset(allergenName)?.aliases.orEmpty() + allergenName
    return keywords.any { keyword ->
        val key = keyword.replace(" ", "")
        key == food || (key.length >= 2 && food.contains(key))
    }
}
