package com.eatda.app.data

import java.time.LocalDate

// 영수증에서 찾을 식재료
// keywords 중 하나가 영수증 줄에 들어 있으면 name으로 입고, 소비기한은 오늘 + shelfDays로 추정
data class ReceiptFood(
    val name: String,
    val shelfDays: Long,
    val keywords: List<String>,
)

// 영수증에서 뽑은 입고 후보 (확인 화면에서 이름·수량·소비기한 수정, 체크 해제 가능)
data class ReceiptEntry(
    val id: Int,
    val name: String,
    val qty: Int,
    val expiry: LocalDate,
    val rawLine: String,          // 인식된 영수증 원문 줄 (확인용)
    val checked: Boolean = true,
)

// 보관 기간은 냉장 보관 기준 대략적인 추정값 (입고 후 재고 상세에서 수정 가능)
val RECEIPT_FOODS: List<ReceiptFood> = listOf(
    // 과일
    ReceiptFood("사과", 30, listOf("사과")),
    ReceiptFood("바나나", 7, listOf("바나나")),
    ReceiptFood("오렌지", 21, listOf("오렌지")),
    ReceiptFood("포도", 7, listOf("포도", "샤인머스캣")),
    ReceiptFood("딸기", 5, listOf("딸기")),
    ReceiptFood("수박", 7, listOf("수박")),
    ReceiptFood("참외", 7, listOf("참외")),
    ReceiptFood("복숭아", 5, listOf("복숭아")),
    ReceiptFood("배", 30, listOf("배", "신고배")),
    ReceiptFood("귤", 14, listOf("귤", "감귤", "한라봉")),
    ReceiptFood("키위", 14, listOf("키위")),
    ReceiptFood("블루베리", 7, listOf("블루베리")),
    ReceiptFood("방울토마토", 7, listOf("방울토마토", "대추토마토")),
    ReceiptFood("토마토", 7, listOf("토마토")),

    // 채소
    ReceiptFood("당근", 21, listOf("당근")),
    ReceiptFood("오이", 7, listOf("오이")),
    ReceiptFood("감자", 30, listOf("감자")),
    ReceiptFood("고구마", 30, listOf("고구마")),
    ReceiptFood("양파", 30, listOf("양파")),
    ReceiptFood("마늘", 30, listOf("마늘", "다진마늘")),
    ReceiptFood("대파", 7, listOf("대파", "쪽파", "파")),
    ReceiptFood("브로콜리", 7, listOf("브로콜리")),
    ReceiptFood("시금치", 3, listOf("시금치")),
    ReceiptFood("상추", 3, listOf("상추")),
    ReceiptFood("깻잎", 5, listOf("깻잎")),
    ReceiptFood("양배추", 14, listOf("양배추")),
    ReceiptFood("배추", 14, listOf("배추", "알배기")),
    ReceiptFood("무", 14, listOf("무")),
    ReceiptFood("애호박", 7, listOf("애호박")),
    ReceiptFood("파프리카", 10, listOf("파프리카")),
    ReceiptFood("버섯", 7, listOf("버섯", "느타리", "새송이", "팽이", "표고")),
    ReceiptFood("콩나물", 3, listOf("콩나물")),
    ReceiptFood("숙주", 3, listOf("숙주")),
    ReceiptFood("고추", 10, listOf("고추", "청양고추", "풋고추")),

    // 유제품·계란·두부
    ReceiptFood("우유", 7, listOf("우유", "딸기우유", "바나나우유", "초코우유")),
    ReceiptFood("계란", 30, listOf("계란", "달걀", "특란", "대란", "왕란")),
    ReceiptFood("두부", 5, listOf("두부")),
    ReceiptFood("두유", 30, listOf("두유")),
    ReceiptFood("치즈", 30, listOf("치즈")),
    ReceiptFood("요거트", 14, listOf("요거트", "요구르트")),
    ReceiptFood("버터", 60, listOf("버터")),

    // 육류·해산물
    ReceiptFood("삼겹살", 3, listOf("삼겹살", "삼겹")),
    ReceiptFood("돼지고기", 3, listOf("돼지", "목살", "앞다리", "뒷다리", "돈육", "항정")),
    ReceiptFood("소고기", 3, listOf("소고기", "쇠고기", "한우", "등심", "안심", "차돌", "불고기", "우둔")),
    ReceiptFood("닭가슴살", 2, listOf("닭가슴살")),
    ReceiptFood("닭고기", 2, listOf("닭", "생닭", "통닭", "닭볶음탕", "닭다리", "닭봉")),
    ReceiptFood("연어", 2, listOf("연어")),
    ReceiptFood("고등어", 2, listOf("고등어")),
    ReceiptFood("오징어", 2, listOf("오징어")),
    ReceiptFood("새우", 2, listOf("새우", "칵테일새우")),
    ReceiptFood("햄", 14, listOf("햄", "스팸")),
    ReceiptFood("소시지", 14, listOf("소시지", "비엔나")),
    ReceiptFood("베이컨", 10, listOf("베이컨")),

    // 가공식품·양념
    ReceiptFood("김치", 30, listOf("김치", "깍두기")),
    ReceiptFood("고추장", 180, listOf("고추장")),
    ReceiptFood("된장", 180, listOf("된장")),
    ReceiptFood("쌈장", 180, listOf("쌈장")),
    ReceiptFood("간장", 365, listOf("간장")),
    ReceiptFood("쌀", 365, listOf("쌀", "백미", "현미")),
    ReceiptFood("즉석밥", 180, listOf("햇반", "즉석밥", "오뚜기밥")),
    ReceiptFood("라면", 180, listOf("라면", "신라면", "진라면", "짜파게티")),
    ReceiptFood("식빵", 5, listOf("식빵", "빵")),
    ReceiptFood("밀가루", 180, listOf("밀가루", "부침가루", "튀김가루")),
)

// 결제·합계·매장 정보처럼 식재료가 아닌 줄
private val EXCLUDE_WORDS = listOf(
    "합계", "총액", "총구매", "부가세", "과세", "면세", "할인", "쿠폰", "포인트", "적립",
    "카드", "현금", "거스름", "봉투", "영수증", "사업자", "전화", "대표", "승인", "결제",
    "주소", "매출", "받을", "반품", "교환", "회원", "잔액", "단가", "수량", "금액", "품목", "상품명",
)

// 긴 키워드부터 비교 ("양배추"를 "배추"보다, "고추장"을 "고추"보다 먼저)
private val KEYWORD_INDEX: List<Pair<String, ReceiptFood>> = RECEIPT_FOODS
    .flatMap { food -> food.keywords.map { it to food } }
    .sortedByDescending { it.first.length }

private val PACK_QTY_REGEX = Regex("""[xX×*]\s*(\d{1,2})(?!\d)""")
private val NUMBER_REGEX = Regex("""\d[\d,]*""")

// 한 글자 키워드("배", "무", "파", "귤", "닭", "햄", "빵", "쌀")는 다른 단어에 섞이지 않을 때만
// 예: "무 1개" O, "무항생제 계란" X / "배 3입" O, "배홍동" X
private fun matchFood(line: String): ReceiptFood? {
    val compact = line.replace(" ", "")
    for ((keyword, food) in KEYWORD_INDEX) {
        val hit = if (keyword.length >= 2) {
            compact.contains(keyword)
        } else {
            Regex("(^|[^가-힣])${Regex.escape(keyword)}([^가-힣]|$)").containsMatchIn(compact)
        }
        if (hit) return food
    }
    return null
}

// 수량 추정
// 1) "단가 수량 금액" 순서로 단가 × 수량 = 금액인 숫자 3개가 있으면 그 수량
// 2) "*3", "x3" 표기
// 3) 찾지 못하면 1
private fun parseQty(line: String): Int {
    val numbers = NUMBER_REGEX.findAll(line)
        .mapNotNull { it.value.replace(",", "").toLongOrNull() }
        .toList()
    for (i in 0..numbers.size - 3) {
        val (price, qty, amount) = Triple(numbers[i], numbers[i + 1], numbers[i + 2])
        if (qty in 1..99 && price >= 100 && price * qty == amount) return qty.toInt()
    }
    PACK_QTY_REGEX.find(line)?.groupValues?.get(1)?.toIntOrNull()?.let { if (it in 1..99) return it }
    return 1
}

// 영수증 OCR 글자 → 입고 후보 목록
// 같은 식재료가 여러 줄이면 수량을 합침
fun parseReceipt(text: String, today: LocalDate = LocalDate.now()): List<ReceiptEntry> {
    val qtyByName = LinkedHashMap<String, Int>()
    val foodByName = HashMap<String, ReceiptFood>()
    val rawByName = HashMap<String, String>()

    text.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .forEach { line ->
            val compact = line.replace(" ", "")
            if (EXCLUDE_WORDS.any { compact.contains(it) }) return@forEach
            val food = matchFood(line) ?: return@forEach
            qtyByName[food.name] = (qtyByName[food.name] ?: 0) + parseQty(line)
            foodByName[food.name] = food
            rawByName.putIfAbsent(food.name, line)
        }

    return qtyByName.entries.mapIndexed { index, (name, qty) ->
        ReceiptEntry(
            id = index,
            name = name,
            qty = qty,
            expiry = today.plusDays(foodByName.getValue(name).shelfDays),
            rawLine = rawByName.getValue(name),
        )
    }
}
