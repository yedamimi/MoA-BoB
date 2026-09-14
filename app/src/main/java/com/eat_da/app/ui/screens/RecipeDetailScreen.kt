package com.eatda.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eatda.app.data.foodEmoji
import com.eatda.app.data.model.Recipe
import com.eatda.app.ui.components.EatdaIcon
import com.eatda.app.ui.components.EatdaIcons
import com.eatda.app.ui.theme.EatdaColors
import com.eatda.app.ui.theme.EatdaSizes

@Composable
fun RecipeDetailScreen(
    colors: EatdaColors,
    sizes: EatdaSizes,
    recipe: Recipe,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 14.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // ─────────────────────────────
        // 상단
        // ─────────────────────────────

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    EatdaIcon(
                        EatdaIcons.ChevronLeft,
                        tint = colors.text,
                        size = 20.dp
                    )
                }

                Text(
                    "레시피 상세",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.text
                )
            }
        }

        // ─────────────────────────────
        // 레시피 기본 정보
        // ─────────────────────────────

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        colors.surface,
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        1.dp,
                        colors.border,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {

                Text(
                    recipe.title,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.text
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    "${recipe.tag} · ${recipe.minutes}분",
                    fontSize = 13.sp,
                    color = colors.textMuted
                )

                if (recipe.description.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))

                    Text(
                        recipe.description,
                        fontSize = 13.sp,
                        color = colors.textMuted
                    )
                }
            }
        }

        // ─────────────────────────────
        // 재료
        // ─────────────────────────────

        item {
            Text(
                "재료",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.text
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        colors.surface,
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        1.dp,
                        colors.border,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                recipe.ingredients.forEach { ingredient ->

                    Text(
                        buildString {
                            val emoji = foodEmoji[ingredient]
                            if (emoji != null) {
                                append("$emoji ")
                            }
                            append(ingredient)
                        },
                        fontSize = 13.sp,
                        color = colors.text
                    )
                }
            }
        }

        // ─────────────────────────────
        // 영양정보
        // ─────────────────────────────

        item {
            Text(
                "영양정보",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.text
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        colors.surface,
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        1.dp,
                        colors.border,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                NutritionRow(
                    colors,
                    "열량",
                    "${recipe.calories} kcal"
                )

                NutritionRow(
                    colors,
                    "탄수화물",
                    "${recipe.carbohydrates} g"
                )

                NutritionRow(
                    colors,
                    "단백질",
                    "${recipe.protein} g"
                )

                NutritionRow(
                    colors,
                    "지방",
                    "${recipe.fat} g"
                )

                NutritionRow(
                    colors,
                    "나트륨",
                    "${recipe.sodium} mg"
                )
            }
        }

        // ─────────────────────────────
        // 조리과정
        // ─────────────────────────────

        item {
            Text(
                "조리과정",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.text
            )
        }

        if (recipe.cookingSteps.isEmpty()) {

            item {
                Text(
                    "등록된 조리과정이 없습니다.",
                    fontSize = 13.sp,
                    color = colors.textMuted
                )
            }

        } else {

            items(
                recipe.cookingSteps
            ) { step ->

                val index =
                    recipe.cookingSteps.indexOf(step) + 1

                CookingStep(
                    colors = colors,
                    number = index,
                    text = step
                )
            }
        }

        item {
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun NutritionRow(
    colors: EatdaColors,
    name: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            name,
            fontSize = 13.sp,
            color = colors.textMuted
        )

        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = colors.text
        )
    }
}

@Composable
private fun CookingStep(
    colors: EatdaColors,
    number: Int,
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                colors.surface,
                RoundedCornerShape(14.dp)
            )
            .border(
                1.dp,
                colors.border,
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Box(
            modifier = Modifier
                .size(30.dp)
                .background(
                    colors.accentSoft,
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                number.toString(),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.accentDeep
            )
        }

        Text(
            text,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = colors.text,
            lineHeight = 20.sp
        )
    }
}