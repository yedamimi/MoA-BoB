package com.eatda.app.data

import com.eatda.app.data.model.*
import java.time.LocalDate

// 목업 데이터 기준일. 앱을 실행한 날(자정이 지나면 다음 날) 기준으로 계산
val TODAY: LocalDate get() = LocalDate.now()

private fun dateAt(days: Int): LocalDate =
    TODAY.plusDays(days.toLong())

val sampleInventory: List<FoodItem> = listOf(
    // Expired
    FoodItem(
        "13",
        "두부",
        FoodCategory.PROCESSED,
        dateAt(-2),
        "1모",
        location = "냉장 2칸",
        addedDays = 9
    ),
    FoodItem(
        "14",
        "우유",
        FoodCategory.DAIRY,
        dateAt(-1),
        "500ml",
        location = "냉장 도어",
        addedDays = 8,
        isAllergen = true
    ),

    // Critical
    FoodItem(
        "1",
        "닭가슴살",
        FoodCategory.MEAT,
        dateAt(1),
        "300g",
        location = "냉장 1칸",
        addedDays = 4
    ),
    FoodItem(
        "2",
        "두부",
        FoodCategory.PROCESSED,
        dateAt(0),
        "1모",
        location = "냉장 2칸",
        addedDays = 5
    ),
    FoodItem(
        "3",
        "딸기",
        FoodCategory.FRUIT,
        dateAt(2),
        "1팩",
        location = "냉장 3칸",
        addedDays = 3
    ),

    // Warning
    FoodItem(
        "4",
        "우유",
        FoodCategory.DAIRY,
        dateAt(4),
        "900ml",
        location = "냉장 도어",
        addedDays = 2,
        isAllergen = true
    ),
    FoodItem(
        "5",
        "계란",
        FoodCategory.DAIRY,
        dateAt(6),
        "10구",
        location = "냉장 도어",
        addedDays = 1,
        isAllergen = true
    ),
    FoodItem(
        "6",
        "양배추",
        FoodCategory.VEGETABLE,
        dateAt(5),
        "1/2통",
        location = "냉장 야채실",
        addedDays = 6
    ),
    FoodItem(
        "7",
        "대파",
        FoodCategory.VEGETABLE,
        dateAt(3),
        "2대",
        location = "냉장 야채실",
        addedDays = 7
    ),

    // Fresh
    FoodItem(
        "8",
        "연어",
        FoodCategory.SEAFOOD,
        dateAt(2),
        "200g",
        location = "냉동실",
        addedDays = 1
    ),
    FoodItem(
        "9",
        "브로콜리",
        FoodCategory.VEGETABLE,
        dateAt(8),
        "1송이",
        location = "냉장 야채실",
        addedDays = 1
    ),
    FoodItem(
        "10",
        "사과",
        FoodCategory.FRUIT,
        dateAt(12),
        "4개",
        location = "냉장 3칸",
        addedDays = 2
    ),
    FoodItem(
        "11",
        "간장",
        FoodCategory.SAUCE,
        dateAt(180),
        "500ml",
        location = "냉장 도어",
        addedDays = 30
    ),
    FoodItem(
        "12",
        "쌀",
        FoodCategory.GRAIN,
        dateAt(60),
        "5kg",
        location = "실온",
        addedDays = 15
    ),
)

val sampleNotifications: List<AppNotification> = listOf(
    AppNotification(
        "n1",
        NotifType.EXPIRY_SOON,
        "두부 유통기한 오늘까지",
        "냉장 2칸에 보관 중입니다.",
        "오늘 오전 9:12",
        urgent = true
    ),
    AppNotification(
        "n2",
        NotifType.EXPIRY_SOON,
        "닭가슴살 내일까지",
        "레시피 추천을 받아보세요.",
        "오늘 오전 8:00",
        urgent = true
    ),
    AppNotification(
        "n3",
        NotifType.ROT,
        "대파 신선도 72%",
        "잎 끝부분 시들음 감지",
        "어제 오후 6:30"
    ),
    AppNotification(
        "n4",
        NotifType.TIP,
        "양배추 보관 팁",
        "키친타올로 분리해 소분하세요.",
        "어제 오후 2:00"
    ),
)

val sampleRecipes: List<Recipe> = listOf(
    // dummy 데이터 제거
)

val defaultAllergens: List<Allergen> = listOf(
    Allergen("a1", "우유", "2026.04.10"),
    Allergen("a2", "계란", "2026.04.10"),
)

val scanResultsIn: List<ScanResult> = listOf(
    ScanResult(
        "s1",
        "브로콜리",
        confidence = 0.982,
        suggestedExpiry = dateAt(7),
        category = FoodCategory.VEGETABLE
    ),
    ScanResult(
        "s2",
        "양파",
        confidence = 0.967,
        suggestedExpiry = dateAt(26),
        category = FoodCategory.VEGETABLE
    ),
    ScanResult(
        "s3",
        "당근",
        confidence = 0.941,
        suggestedExpiry = dateAt(19),
        category = FoodCategory.VEGETABLE
    ),
)

val scanResultsOut: List<ScanResult> = listOf(
    ScanResult("o1", "삼겹살", qty = "300g"),
    ScanResult("o2", "계란", qty = "6개", isAllergen = true),
)

val scanResultsFresh: List<ScanResult> = listOf(
    ScanResult(
        "f1",
        "딸기",
        freshness = 62,
        rotPct = 18,
        freshnessStatus = FreshnessStatus.ATTENTION
    ),
    ScanResult(
        "f2",
        "양배추",
        freshness = 84,
        rotPct = 4,
        freshnessStatus = FreshnessStatus.FRESH
    ),
)

val foodEmoji: Map<String, String> = mapOf(
    "닭가슴살" to "🍗",
    "두부" to "🧈",
    "오이" to "🥒",
    "딸기" to "🍓",
    "우유" to "🥛",
    "계란" to "🥚",
    "양배추" to "🥬",
    "대파" to "🌿",
    "연어" to "🐟",
    "브로콜리" to "🥦",
    "사과" to "🍎",
    "간장" to "🍶",
    "쌀" to "🌾",
    "삼겹살" to "🥩",
    "양파" to "🧅",
    "당근" to "🥕",
    "토마토" to "🍅",
    "바나나" to "🍌",
    "오렌지" to "🍊",
    "샌드위치" to "🥪",
    "핫도그" to "🌭",
    "피자" to "🍕",
    "도넛" to "🍩",
    "케이크" to "🎂",
)

fun storageGuide(name: String): String = when (name) {

    "오이" ->
        "① 물기를 깨끗이 제거하세요.\n" +
                "② 키친타월로 감싸 수분 손실을 줄여주세요.\n" +
                "③ 밀폐용기나 비닐팩에 넣어 냉장 보관하세요.\n" +
                "※ 물기가 오래 남아 있으면 쉽게 무를 수 있습니다."

    "바나나" ->
        "① 서늘하고 통풍이 잘 되는 실온에 보관하세요.\n" +
                "② 냉장 보관은 껍질이 빠르게 검게 변할 수 있어 피해주세요.\n" +
                "③ 송이의 꼭지 부분을 랩이나 비닐로 감싸주세요.\n" +
                "※ 꼭지를 감싸면 숙성 속도를 늦추는 데 도움이 됩니다."

    "양파" ->
        "① 껍질이 있는 통양파는 물기를 제거하세요.\n" +
                "② 망이나 바구니처럼 통풍이 잘 되는 곳에 보관하세요.\n" +
                "③ 서늘하고 건조한 곳에서 보관하세요.\n" +
                "※ 자른 양파는 밀폐용기에 넣어 냉장 보관하세요."

    "당근" ->
        "① 표면의 물기를 깨끗하게 제거하세요.\n" +
                "② 신문지나 키친타월로 감싸주세요.\n" +
                "③ 냉장고에 넣어 보관하세요.\n" +
                "※ 잎이 붙어 있다면 잎을 제거한 후 보관하는 것이 좋습니다."

    "무" ->
        "① 무에 붙어 있는 잎을 먼저 제거하세요.\n" +
                "② 표면의 물기를 제거해주세요.\n" +
                "③ 신문지나 키친타월로 감싼 뒤 냉장 보관하세요.\n" +
                "※ 잎을 그대로 두면 무의 수분이 빠르게 줄어들 수 있습니다."

    "대파" ->
        "① 대파의 물기를 깨끗하게 제거하세요.\n" +
                "② 키친타월이나 신문지로 감싸주세요.\n" +
                "③ 밀폐용기나 지퍼백에 넣어 냉장 보관하세요.\n" +
                "※ 장기간 보관할 경우 먹기 좋은 크기로 잘라 냉동 보관할 수 있습니다."

    "파프리카" ->
        "① 표면의 물기를 깨끗하게 제거하세요.\n" +
                "② 키친타월로 감싸주세요.\n" +
                "③ 밀폐용기나 지퍼백에 넣어 냉장 보관하세요.\n" +
                "※ 물기가 오래 남아 있으면 쉽게 물러질 수 있습니다."

    "방울토마토" ->
        "① 완숙 전에는 실온의 서늘한 곳에 보관하세요.\n" +
                "② 충분히 익은 후에는 냉장 보관하세요.\n" +
                "③ 냉장 보관 전 표면의 물기를 제거해주세요.\n" +
                "※ 너무 많이 겹쳐 보관하면 쉽게 물러질 수 있습니다."

    "브로콜리" ->
        "① 브로콜리의 물기를 깨끗하게 제거하세요.\n" +
                "② 키친타월로 감싸주세요.\n" +
                "③ 냉장 보관하고 가능한 한 빠르게 섭취하세요.\n" +
                "※ 오래 보관하면 꽃봉오리가 노랗게 변하고 식감이 떨어질 수 있습니다."

    "애호박" ->
        "① 표면의 물기를 깨끗하게 제거하세요.\n" +
                "② 신문지나 키친타월로 감싸주세요.\n" +
                "③ 냉장 보관하세요.\n" +
                "※ 자른 애호박은 단면을 랩으로 감싸 밀폐용기에 보관하세요."

    "사과" ->
        "① 사과 표면의 상태를 확인해주세요.\n" +
                "② 냉장고에 넣어 보관하면 신선도를 오래 유지할 수 있습니다.\n" +
                "③ 상처가 있거나 물러진 사과는 먼저 섭취하세요.\n" +
                "※ 다른 과일·채소와 함께 보관하면 숙성이 빨라질 수 있습니다."

    "삼겹살" ->
        "① 표면의 상태를 확인해주세요.\n" +
                "② 냉장고에 넣어 보관하세요. 신선도를 오래 유지할 수 있습니다.\n"

    "연어" ->
        "① 표면의 상태를 확인해주세요.\n" +
                "② 냉장고에 넣어 보관하세요. 신선도를 오래 유지할 수 있습니다.\n"

    "우유" ->
        "냉장고에 넣어 보관하세요. 신선도를 오래 유지할 수 있습니다."

    "아이셔" ->
        "① 포장지에 나와 있는 보관 방법을 확인하세요."

    "달콤연근조림" ->
        "① 포장지에 나와 있는 보관 방법을 확인하세요."

    else ->
        "① 식재료의 상태를 확인해주세요.\n" +
                "② 물기가 있다면 깨끗하게 제거해주세요.\n" +
                "③ 식재료에 맞는 적절한 환경에서 보관하세요."
}


// ═══════════════════════════════════════
// 레시피 재료 매칭
// ═══════════════════════════════════════

// 레시피 재료명 끝에 붙은 수량을 제거하기 위한 Regex
//
// 예:
// "연어 200g" → "연어"
// "두부 1모" → "두부"
// "브로콜리 1/2송이" → "브로콜리"
// "소금 약간" → "소금"
private val INGREDIENT_QUANTITY_REGEX = Regex(
    """(?:\(?\d+(?:\.\d+)?(?:/\d+)?\s*(?:kg|g|mg|ml|l|개|모|팩|봉지|봉|장|대|통|구|병|캔|컵|인분|송이|큰술|작은술|숟가락|스푼|쪽|줌|꼬집|알|마리|조각|t)\)?|약간|적당량|조금|소량)$""",
    RegexOption.IGNORE_CASE
)

// 공백·수량을 제거해 재료 비교용 키로 변환
//
// 냉장고 재료와 레시피 재료는 이 키가 정확히 같을 때만 매칭으로 봄
private fun ingredientKey(value: String): String =
    INGREDIENT_QUANTITY_REGEX
        .replace(
            value
                .trim()
                .replace(" ", "")
                .replace("　", ""),
            ""
        )
        .trim()

private data class ScoredRecipe(
    val recipe: Recipe,
    val totalScore: Double,
    val hasPriorityIngredient: Boolean,
    val hasExpiringIngredient: Boolean,
    val categoryScore: Int,
)

fun recommendRecipes(
    availableIngredients: List<String>,
    recipes: List<Recipe>,
    inventory: List<FoodItem>,
    lackingNutrient: String? = "단백질",
): List<Recipe> {

    val availableKeys = availableIngredients
        .map(::ingredientKey)
        .filter { it.isNotBlank() }
        .toSet()

    // 소비기한이 임박한 재료를 임박한 순서로 정렬
    // 이미 만료된 재료는 섭취를 권장하지 않으므로 우선순위에서 제외
    val expiringIngredients = inventory
        .filter {
            it.expiryStatus() in listOf(
                ExpiryStatus.CRITICAL,
                ExpiryStatus.WARNING
            )
        }
        .sortedBy { it.daysLeft() }
        .map { ingredientKey(it.name) }
        .filter { it.isNotBlank() }
        .distinct()

    // 가장 먼저 소진해야 하는 재료
    val priorityIngredient = expiringIngredients.firstOrNull()

    return recipes
        .map { recipe ->

            val recipeKeys = recipe.ingredients
                .map(::ingredientKey)
                .filter { it.isNotBlank() }

            val ingredientCount = recipe.ingredients.size

            val matchedCount = recipeKeys.count { it in availableKeys }

            val expiringCount = recipeKeys.count { it in expiringIngredients }

            val ingredientScore =
                if (ingredientCount > 0) matchedCount.toDouble() / ingredientCount else 0.0

            val expiryScore =
                if (ingredientCount > 0) expiringCount.toDouble() / ingredientCount else 0.0

            val nutritionScore =
                if (lackingNutrient != null && recipe.tag == lackingNutrient) 1.0 else 0.0

            val categoryScore = when {
                matchedCount == ingredientCount -> 5
                matchedCount >= 4 -> 4
                else -> matchedCount
            }

            ScoredRecipe(
                recipe = recipe,
                totalScore = ingredientScore * 0.5 +
                        nutritionScore * 0.3 +
                        expiryScore * 0.2,
                hasPriorityIngredient = priorityIngredient != null &&
                        priorityIngredient in recipeKeys,
                hasExpiringIngredient = expiringCount > 0,
                categoryScore = categoryScore,
            )
        }
        .filter { it.categoryScore > 0 && it.recipe.ingredients.isNotEmpty() }
        .sortedWith(
            // ① 가장 임박한 재료가 포함된 레시피를 최우선
            compareByDescending<ScoredRecipe> { it.hasPriorityIngredient }
                // ② 그 외 소비기한 임박 재료가 포함된 레시피
                .thenByDescending { it.hasExpiringIngredient }
                // ③ 추천 점수
                .thenByDescending { it.totalScore }
                // ④ 카테고리 점수
                .thenByDescending { it.categoryScore }
        )
        .map { it.recipe }
}
