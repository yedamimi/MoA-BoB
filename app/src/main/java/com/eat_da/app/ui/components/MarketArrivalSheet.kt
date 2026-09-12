package com.eatda.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.eatda.app.data.*
import com.eatda.app.data.model.*
import com.eatda.app.ui.theme.*

/* ────────────────────────────────────────────────────────────
   MarketArrivalSheet — 싱싱마켓 구매 수신 팝업
   itda-shop.jsx PurchasePopup 재현 (Jetpack Compose)
   ──────────────────────────────────────────────────────────── */
@Composable
fun MarketArrivalSheet(
    colors:    EatdaColors,
    items:     List<FoodItem>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val allergenHits = items.filter { it.isAllergen }
    val totalCount   = items.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        /* 딤 배경 */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.overlay),
            contentAlignment = Alignment.Center,
        ) {
            /* 카드 */
            Column(
                modifier = Modifier
                    .widthIn(max = 330.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .padding(top = 20.dp, start = 18.dp, end = 18.dp, bottom = 18.dp),
            ) {

                /* 1. 출처 배지 */
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.surfaceAlt)
                        .padding(horizontal = 11.dp, vertical = 5.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text("📦", fontSize = 12.sp)
                        Text(
                            "싱싱마켓에서 도착",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                /* 2. 제목 */
                Text(
                    "구매한 식재료가 있어요",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.text,
                )

                Spacer(Modifier.height(4.dp))

                /* 3. 본문 */
                Text(
                    "${totalCount}개 상품을\n모아밥 냉장고 재고에 넣을까요?",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = colors.textMuted,
                    lineHeight = 20.sp,
                )

                Spacer(Modifier.height(14.dp))

                /* 4. 항목 리스트 */
                Column(
                    modifier = Modifier
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items.take(5).forEach { item ->
                        MarketItemRow(colors = colors, item = item)
                    }
                }

                /* 5. 알레르기 경고 */
                if (allergenHits.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.dangerSoft)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("⚠️", fontSize = 16.sp)
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(allergenHits.joinToString(", ") { it.name })
                                }
                                append(" — 등록된 알레르기 재료가 포함되어 있어요")
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.danger,
                            lineHeight = 16.sp,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                /* 6. 버튼 행 */
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    /* 나중에 */
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                            .background(colors.surface)
                            .height(48.dp),
                    ) {
                        Text(
                            "나중에",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted,
                        )
                    }

                    /* 모아밥 냉장고에 넣기 */
                    TextButton(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(2f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.accent)
                            .height(48.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("🧊", fontSize = 16.sp)
                            Text(
                                "모아밥 냉장고에 넣기",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ── 항목 행 ── */
@Composable
private fun MarketItemRow(colors: EatdaColors, item: FoodItem) {
    val cat      = item.category
    val catColor = Color(cat.hexColor)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceAlt)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        /* 이모지 박스 */
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(catColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                foodEmoji[item.name] ?: item.name.first().toString(),
                fontSize = 16.sp,
            )
        }

        /* 이름 + 메타 */
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.name,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text,
            )
            Text(
                buildString {
                    append(item.qty ?: "1개")
                    val dl = item.daysLeft()
                    if (dl >= 0) append(" · D-$dl")
                },
                fontSize = 10.sp,
                color = colors.textFaint,
                modifier = Modifier.padding(top = 1.dp),
            )
        }

        /* 화살표 */
        Text("›", fontSize = 14.sp, color = colors.textFaint)
    }
}
