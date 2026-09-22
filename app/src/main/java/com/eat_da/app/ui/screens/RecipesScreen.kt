package com.eatda.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eatda.app.data.foodEmoji
import com.eatda.app.data.recommendRecipes
import com.eatda.app.data.model.ExpiryStatus
import com.eatda.app.data.model.FoodItem
import com.eatda.app.data.model.Recipe
import com.eatda.app.ui.components.*
import com.eatda.app.ui.theme.*
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipesScreen(
    colors: EatdaColors,
    sizes: EatdaSizes,
    inventory: List<FoodItem>,
    recipes: List<Recipe>,
    onRecipeClick: (Recipe) -> Unit,
    recipeFocusIngredient: String? = null,
) {

    // ─────────────────────────────
    // 레시피 카테고리 선택 상태
    //
    // 0 = 전체
    // 1 = 모든 재료
    // 2 = 4개 이상
    // 3 = 3개
    // 4 = 2개
    // 5 = 1개
    // ─────────────────────────────

    val selectedCategory = remember { mutableStateOf(0) }

    // ─────────────────────────────
    // 재료명 정규화
    //
    // "연어", " 연어 ", "연 어"처럼
    // 불필요한 공백 차이 때문에
    // 재료 매칭이 실패하지 않도록 처리
    // ─────────────────────────────

    fun normalizeIngredient(name: String): String {
        return name
            .trim()
            .replace(" ", "")
            .replace("　", "")
    }

    // ─────────────────────────────
    // 레시피 재료 ↔ 실제 재고 재료 매칭
    //
    // 예:
    // 레시피: "연어 200g"
    // 재고:   "연어"
    //
    // → 같은 재료로 판단
    // ─────────────────────────────

    fun isIngredientMatched(
        recipeIngredient: String,
        inventoryIngredient: String
    ): Boolean {

        fun removeQuantity(name: String): String {
            return name
                .replace(
                    Regex(
                        """\(?\d+(?:\.\d+)?(?:/\d+)?\s*(?:kg|g|mg|ml|l|개|모|팩|봉|장|대|통|구|병|캔|컵|인분)\)?$"""
                    ),
                    ""
                )
                .trim()
        }

        val recipeName = removeQuantity(
            normalizeIngredient(recipeIngredient)
        )

        val inventoryName = removeQuantity(
            normalizeIngredient(inventoryIngredient)
        )

        if (recipeName.isBlank() || inventoryName.isBlank()) {
            return false
        }

        return recipeName == inventoryName
    }

    // ─────────────────────────────
    // 하나의 레시피에서
    // 현재 Firebase 재고와 일치하는
    // 재료가 몇 개인지 계산
    // ─────────────────────────────

    fun matchedIngredientCount(recipe: Recipe): Int {
        return recipe.ingredients
            .distinct()
            .count { ingredient ->
                inventory.any { item ->
                    isIngredientMatched(
                        ingredient,
                        item.name
                    )
                }
            }
    }

    // ─────────────────────────────
    // 추천 레시피
    //
    // recipeFocusIngredient가 있으면
    // 특정 재료를 눌러 들어온 상태이므로
    // 해당 재료가 들어간 레시피를 보여줌.
    //
    // 일반 레시피 화면에서는
    // 실제 Firebase 재고를 기반으로
    // recommendRecipes()를 사용.
    // ─────────────────────────────

    val recommended = if (recipeFocusIngredient != null) {

        recipes
            .filter { recipe ->
                recipe.ingredients.any { ingredient ->
                    isIngredientMatched(
                        ingredient,
                        recipeFocusIngredient
                    )
                }
            }
            .take(3)

    } else {

        recommendRecipes(
            inventory.map { it.name },
            recipes = recipes,
            inventory = inventory
        )
    }

    // ─────────────────────────────
    // 카테고리별 필터
    // ─────────────────────────────

    val filteredRecipes = when (selectedCategory.value) {

        // ─────────────────────────
        // 전체
        // ─────────────────────────
        0 -> recommended

        // ─────────────────────────
        // 모든 재료
        //
        // 레시피에 필요한 재료를
        // 전부 현재 재고에서 가지고 있는 경우
        // ─────────────────────────
        1 -> recommended.filter { recipe ->

            recipe.ingredients.isNotEmpty() &&
                    matchedIngredientCount(recipe) ==
                    recipe.ingredients.size
        }

        // ─────────────────────────
        // 4개 이상
        //
        // 4개 이상 가지고 있지만
        // 모든 재료를 가지고 있는 것은 아닌 경우
        // ─────────────────────────
        2 -> recommended.filter { recipe ->

            val matchedCount =
                matchedIngredientCount(recipe)

            matchedCount >= 4 &&
                    matchedCount < recipe.ingredients.size
        }

        // ─────────────────────────
        // 3개
        // ─────────────────────────
        3 -> recommended.filter { recipe ->

            val matchedCount =
                matchedIngredientCount(recipe)

            matchedCount == 3 &&
                    matchedCount < recipe.ingredients.size
        }

        // ─────────────────────────
        // 2개
        // ─────────────────────────
        4 -> recommended.filter { recipe ->

            val matchedCount =
                matchedIngredientCount(recipe)

            matchedCount == 2 &&
                    matchedCount < recipe.ingredients.size
        }

        // ─────────────────────────
        // 1개
        // ─────────────────────────
        5 -> recommended.filter { recipe ->

            val matchedCount =
                matchedIngredientCount(recipe)

            matchedCount == 1 &&
                    matchedCount < recipe.ingredients.size
        }

        else -> recommended
    }

    // ─────────────────────────────
    // 유통기한 임박 재료
    //
    // 실제 Firebase inventory를 기준으로
    // EXPIRED / CRITICAL / WARNING 상태인
    // 재료를 가져옴.
    // ─────────────────────────────

    val expiringItems = inventory
        .filter {
            it.expiryStatus() in listOf(
                ExpiryStatus.EXPIRED,
                ExpiryStatus.CRITICAL,
                ExpiryStatus.WARNING
            )
        }
        .sortedBy { it.daysLeft() }
        .take(4)

    // ─────────────────────────────
    // 화면
    // ─────────────────────────────

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 0.dp
        ),
    ) {

        // ─────────────────────────────
        // 제목
        // ─────────────────────────────

        item {

            Text(
                "레시피",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.text,
                modifier = Modifier.padding(
                    horizontal = 4.dp,
                    vertical = 14.dp
                )
            )
        }

        // ─────────────────────────────
        // 레시피 카테고리
        // ─────────────────────────────

        item {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .horizontalScroll(
                        androidx.compose.foundation
                            .rememberScrollState()
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                val categories = listOf(
                    "전체",
                    "모든 재료",
                    "4개 이상",
                    "3개",
                    "2개",
                    "1개"
                )

                categories.forEachIndexed {
                        index,
                        category ->

                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(999.dp)
                            )
                            .background(
                                if (
                                    selectedCategory.value ==
                                    index
                                ) {
                                    colors.accent
                                } else {
                                    colors.surface
                                }
                            )
                            .border(
                                1.dp,
                                if (
                                    selectedCategory.value ==
                                    index
                                ) {
                                    colors.accent
                                } else {
                                    colors.border
                                },
                                RoundedCornerShape(999.dp)
                            )
                            .clickable {
                                selectedCategory.value = index
                            }
                            .padding(
                                horizontal = 14.dp,
                                vertical = 7.dp
                            )
                    ) {

                        Text(
                            text = category,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color =
                                if (
                                    selectedCategory.value ==
                                    index
                                ) {
                                    Color.White
                                } else {
                                    colors.text
                                }
                        )
                    }
                }
            }
        }

        // ─────────────────────────────
        // 유통기한 임박 안내
        // ─────────────────────────────

        item {

            if (expiringItems.isNotEmpty()) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(16.dp)
                        )
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    colors.accent,
                                    colors.accentDeep
                                )
                            )
                        )
                        .padding(14.dp)
                        .padding(bottom = 14.dp),
                ) {

                    Text(
                        "오늘 우선 소진",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color =
                            Color.White.copy(
                                alpha = 0.85f
                            )
                    )

                    Spacer(
                        Modifier.height(2.dp)
                    )

                    Text(
                        expiringItems.joinToString(" · ") {
                            it.name
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(
                        Modifier.height(4.dp)
                    )

                    Text(
                        when (
                            selectedCategory.value
                        ) {

                            0 ->
                                "냉장고 재료를 활용해 만들 수 있는 추천 메뉴예요"

                            1 ->
                                "냉장고에 있는 재료만으로 만들 수 있어요"

                            2 ->
                                "냉장고 재료를 4개 이상 활용할 수 있어요"

                            3 ->
                                "냉장고 재료 3개를 활용할 수 있어요"

                            4 ->
                                "냉장고 재료 2개를 활용할 수 있어요"

                            5 ->
                                "냉장고 재료 1개를 활용할 수 있어요"

                            else ->
                                "냉장고 재료를 활용해 만들 수 있는 추천 메뉴예요"
                        },
                        fontSize = 12.sp,
                        color =
                            Color.White.copy(
                                alpha = 0.85f
                            )
                    )
                }

            } else {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(16.dp)
                        )
                        .background(
                            colors.accentSoft
                        )
                        .border(
                            1.dp,
                            colors.accent.copy(
                                alpha = 0.2f
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp),
                ) {

                    Text(
                        "임박 재료 없음",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accentDeep
                    )

                    Spacer(
                        Modifier.height(2.dp)
                    )

                    Text(
                        "재고가 모두 신선합니다 🎉",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.accentDeep
                    )
                }
            }

            Spacer(
                Modifier.height(14.dp)
            )
        }

        // ─────────────────────────────
        // 추천 레시피
        // ─────────────────────────────

        if (recommended.isEmpty()) {

            item {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally,
                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        EatdaIcon(
                            EatdaIcons.Leaf,
                            tint = colors.textFaint,
                            size = 36.dp
                        )

                        Text(
                            "추천 레시피가 없습니다",
                            fontSize = 14.sp,
                            color = colors.textFaint
                        )

                        Text(
                            "재고를 추가하면 맞춤 레시피를 추천해 드립니다",
                            fontSize = 12.sp,
                            color = colors.textFaint
                        )
                    }
                }
            }

        } else if (filteredRecipes.isEmpty()) {

            // 선택한 카테고리에 해당하는
            // 레시피가 없는 경우

            item {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally,
                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        EatdaIcon(
                            EatdaIcons.Leaf,
                            tint = colors.textFaint,
                            size = 36.dp
                        )

                        Text(
                            "해당하는 레시피가 없습니다",
                            fontSize = 14.sp,
                            color = colors.textFaint
                        )

                        Text(
                            "다른 재료 카테고리를 선택해 보세요",
                            fontSize = 12.sp,
                            color = colors.textFaint
                        )
                    }
                }
            }

        } else {

            items(filteredRecipes) { recipe ->

                RecipeCard(
                    colors = colors,
                    recipe = recipe,
                    onClick = {
                        onRecipeClick(recipe)
                    }
                )

                Spacer(
                    Modifier.height(10.dp)
                )
            }
        }

        item {
            Spacer(
                Modifier.height(20.dp)
            )
        }
    }
}


// ═══════════════════════════════════════
// 레시피 카드
// ═══════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeCard(
    colors: EatdaColors,
    recipe: Recipe,
    onClick: () -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(16.dp)
            )
            .background(colors.surface)
            .border(
                1.dp,
                colors.border,
                RoundedCornerShape(16.dp)
            )
            .clickable {
                onClick()
            },
    ) {

        // ─────────────────────────────
        // 카드 상단 이미지 영역
        // ─────────────────────────────

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.accentSoft,
                            colors.surfaceAlt
                        )
                    )
                ),
        ) {

            // 식품안전나라 대표 음식 사진
            if (recipe.imageUrl.isNotBlank()) {

                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = recipe.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            // API에서 priority 정보가 있는 경우
            // 기존 배지 표시
            if (recipe.priority != null) {

                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(
                            RoundedCornerShape(999.dp)
                        )
                        .background(colors.danger)
                        .padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        )
                ) {

                    Text(
                        recipe.priority,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // ─────────────────────────────
        // 카드 내용
        // ─────────────────────────────

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            // 제목 + 화살표
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.Top
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        recipe.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )

                    Text(
                        "${recipe.tag} · ${recipe.minutes}분",
                        fontSize = 12.sp,
                        color = colors.textMuted,
                        modifier = Modifier.padding(
                            top = 2.dp
                        )
                    )
                }

                // 오른쪽 화살표
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(
                            1.dp,
                            colors.border,
                            CircleShape
                        )
                        .clickable {
                            onClick()
                        },
                    contentAlignment =
                        Alignment.Center
                ) {

                    EatdaIcon(
                        EatdaIcons.ChevronRight,
                        tint = colors.text,
                        size = 16.dp
                    )
                }
            }

            Spacer(
                Modifier.height(10.dp)
            )

            // ─────────────────────────────
            // 재료 태그
            // ─────────────────────────────

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp),
                verticalArrangement =
                    Arrangement.spacedBy(6.dp),
            ) {

                recipe.ingredients.forEach { ing ->

                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(999.dp)
                            )
                            .background(
                                colors.accentSoft
                            )
                            .padding(
                                horizontal = 10.dp,
                                vertical = 4.dp
                            )
                    ) {

                        val emoji = foodEmoji[ing]

                        Text(
                            text = if (emoji != null) {
                                "$emoji $ing"
                            } else {
                                ing
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accentDeep,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}