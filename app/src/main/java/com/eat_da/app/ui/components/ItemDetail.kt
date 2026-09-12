package com.eatda.app.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eatda.app.data.*
import com.eatda.app.data.model.*
import com.eatda.app.ui.theme.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailSheet(
    colors: EatdaColors,
    item: FoodItem,
    sheetState: SheetState,
    onDismiss: () -> Unit,
) {
    var showStorageGuide by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bg,
        contentColor = colors.text,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val cat = item.category
            val dl = item.daysLeft()
            val freshStatus = item.freshnessStatus()
            val freshnessColor = when {
                (item.freshness ?: 0) >= 80 -> colors.accent
                (item.freshness ?: 0) >= 60 -> colors.warning
                else -> colors.danger
            }
            val expiryStatus = item.expiryStatus()
            val expiryColor = when (expiryStatus) {
                ExpiryStatus.CRITICAL, ExpiryStatus.EXPIRED -> colors.danger
                ExpiryStatus.WARNING -> Color(0xFF9B6B1F)
                else -> colors.text
            }

            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(bottom = 6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(cat.hexColor).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(foodEmoji[item.name] ?: item.name.first().toString(), fontSize = 32.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(item.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = colors.text)
                        if (item.isAllergen) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.dangerSoft)
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                            ) {
                                Text("알레르기", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = colors.danger)
                            }
                        }
                    }
                    Text("${cat.label} · ${item.qty}", fontSize = 12.sp, color = colors.textMuted, modifier = Modifier.padding(top = 2.dp))
                }
            }

            // Expiry card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 4.dp),
                ) {
                    EatdaIcon(EatdaIcons.Clock, tint = colors.textMuted, size = 14.dp)
                    Text("유통기한", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted)
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.formatExpiry(), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = expiryColor)
                    Text(item.expiry.toString(), fontSize = 12.sp, color = colors.textMuted)
                }
                Text(
                    when {
                        dl <= 0 -> "식약처 DB 기준 권장 소비기한 경과"
                        dl <= 1 -> "D-1 자동 알림 발송됨"
                        else -> "구매 후 ${item.addedDays}일 경과"
                    },
                    fontSize = 11.sp,
                    color = colors.textFaint,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // Freshness card
            if (item.freshness != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            EatdaIcon(EatdaIcons.Leaf, tint = colors.textMuted, size = 14.dp)
                            Text("신선도 분석 (OpenCV)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted)
                        }
                        Text("웹캠 스캔 결과", fontSize = 11.sp, color = colors.textFaint)
                    }
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        Text("${item.freshness}%", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = freshnessColor)
                        Text(
                            when (freshStatus) {
                                FreshnessStatus.FRESH     -> "신선"
                                FreshnessStatus.ATTENTION -> "주의"
                                FreshnessStatus.ROTTEN    -> "위험"
                                null                      -> ""
                            },
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = freshnessColor,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.surfaceAlt),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(item.freshness / 100f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(999.dp))
                                .background(freshnessColor),
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceAlt)
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    EatdaIcon(EatdaIcons.Camera, tint = colors.textMuted, size = 18.dp)
                    Column {
                        Text("신선도 미분석", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
                        Text("홈 화면 카메라 버튼 → 신선도 확인 스캔", fontSize = 11.sp, color = colors.textMuted, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }

            // Storage guide
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accentSoft)
                    .clickable {
                        showStorageGuide = true
                    }
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        EatdaIcon(
                            EatdaIcons.Sparkle,
                            tint = colors.accentDeep,
                            size = 14.dp
                        )

                        Text(
                            "맞춤 보관 가이드",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accentDeep
                        )
                    }

                    Text(
                        "›",
                        fontSize = 20.sp,
                        color = colors.accentDeep
                    )
                }

                Text(
                    text = storageGuideSummary(item.name),
                    fontSize = 13.sp,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Storage guide popup
            // Dialog 자체를 전체 화면 크기로 사용하고, 내부 Box에서 중앙 정렬합니다.
            if (showStorageGuide) {
                Dialog(
                    onDismissRequest = {
                        showStorageGuide = false
                    },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                    ),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            modifier = Modifier
                                .width(320.dp)
                                .heightIn(min = 300.dp, max = 420.dp)
                                .border(
                                    width = 1.dp,
                                    color = colors.border,
                                    shape = RoundedCornerShape(20.dp),
                                ),
                            shape = RoundedCornerShape(20.dp),
                            color = colors.bg,
                            shadowElevation = 12.dp,
                            tonalElevation = 6.dp,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                            ) {
                                // 팝업 헤더
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.accentSoft),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        EatdaIcon(
                                            EatdaIcons.Sparkle,
                                            tint = colors.accentDeep,
                                            size = 20.dp,
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "맞춤 보관 가이드",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colors.text,
                                        )

                                        Text(
                                            text = "${foodEmoji[item.name] ?: ""} ${item.name}",
                                            fontSize = 12.sp,
                                            color = colors.textMuted,
                                            modifier = Modifier.padding(top = 2.dp),
                                        )
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                HorizontalDivider(color = colors.border)

                                Spacer(Modifier.height(16.dp))

                                // 상세 내용
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState()),
                                ) {
                                    Text(
                                        text = storageGuide(item.name),
                                        fontSize = 14.sp,
                                        lineHeight = 23.sp,
                                        color = colors.text,
                                    )
                                }

                                Spacer(Modifier.height(16.dp))

                                // 확인 버튼
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.accent)
                                        .clickable {
                                            showStorageGuide = false
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "확인",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("닫기", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
                }
                Row(
                    modifier = Modifier
                        .weight(2f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.accent)
                        .clickable { }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EatdaIcon(EatdaIcons.Leaf, tint = Color.White, size = 16.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("레시피 추천", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
fun storageGuideSummary(name: String): String {
    return when (name) {
        "오이" -> "냉장 보관하고, 물기가 닿지 않도록 보관하세요."
        "바나나" -> "실온에서 보관하고, 꼭지를 감싸면 숙성을 늦출 수 있어요."
        "yangpa" -> "통풍이 잘 되는 서늘하고 건조한 곳에 보관하세요."
        "당근" -> "냉장 보관하고, 신문지나 키친타월로 감싸주세요."
        "무" -> "잎을 제거한 뒤 냉장 보관하면 더 오래 보관할 수 있어요."
        "대파" -> "물기를 제거하고 밀폐 용기에 넣어 냉장 보관하세요."
        "파프리카" -> "물기를 제거한 뒤 냉장 보관하세요."
        "방울토마토" -> "완숙 전에는 실온, 완숙 후에는 냉장 보관하세요."
        "브로콜리" -> "냉장 보관하고, 되도록 빠르게 섭취하세요."
        "애호박" -> "신문지나 키친타월로 감싸 냉장 보관하세요."
        "사과" -> "냉장 보관하면 신선도를 오래 유지할 수 있어요."
        else -> "신선도를 유지할 수 있도록 적절한 환경에서 보관하세요."
    }
}