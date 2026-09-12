package com.eatda.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.eatda.app.data.model.FoodCategory
import com.eatda.app.data.model.FoodItem
import com.eatda.app.ui.theme.EatdaColors
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun MarketArrivalSheet(
    items: List<FoodItem>,
    colors: EatdaColors,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (items.isEmpty()) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(colors.surface)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 출처 뱃지
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(colors.accentSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🛒", fontSize = 12.sp)
                    }
                    Text(
                        "싱싱마켓에서 도착",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMuted,
                    )
                }

                // 제목
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "구매한 식재료가 있어요",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text,
                    )
                    Text(
                        "${items.size}개 상품을 마이 냉장고 재고에 넣을까요?",
                        fontSize = 13.sp,
                        color = colors.textMuted,
                    )
                }

                // 상품 목록
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.take(3).forEach { item ->
                        MarketItemRow(item = item, colors = colors)
                    }
                    if (items.size > 3) {
                        Text(
                            "외 ${items.size - 3}개 상품",
                            fontSize = 12.sp,
                            color = colors.textMuted,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }

                // 알레르기 경고
                if (items.any { it.isAllergen }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.dangerSoft)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("⚠️", fontSize = 14.sp)
                        Text(
                            items.filter { it.isAllergen }.joinToString(", ") { it.name } +
                                    " — 등록된 알레르기 재료가 포함되어 있어요",
                            fontSize = 12.sp,
                            color = colors.danger,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                // 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // 취소 버튼
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(colors.surfaceAlt)
                            .height(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text("나중에", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted)
                        }
                    }
                    // 확인 버튼
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(colors.accent)
                            .height(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        TextButton(
                            onClick = onConfirm,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text("🔒 마이 냉장고에 넣기", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MarketItemRow(item: FoodItem, colors: EatdaColors) {
    val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), item.expiry)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceAlt)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(categoryEmoji(item.category), fontSize = 24.sp)
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${item.qty} · 소비기한 D-$daysLeft",
                fontSize = 11.sp,
                color = colors.textMuted,
            )
        }
        Text("›", fontSize = 18.sp, color = colors.textFaint)
    }
}

private fun categoryEmoji(category: FoodCategory) = when (category) {
    FoodCategory.VEGETABLE -> "🥬"
    FoodCategory.FRUIT     -> "🍎"
    FoodCategory.MEAT      -> "🥩"
    FoodCategory.SEAFOOD   -> "🐟"
    FoodCategory.DAIRY     -> "🥛"
    FoodCategory.GRAIN     -> "🌾"
    FoodCategory.BEVERAGE  -> "🧃"
    FoodCategory.SAUCE     -> "🫙"
}
