package com.eatda.app.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.eatda.app.data.*
import com.eatda.app.data.model.*
import com.eatda.app.ui.components.*
import com.eatda.app.ui.theme.*
import com.eatda.app.util.VisionService
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    colors: EatdaColors,
    sizes: EatdaSizes,
    inventory: List<FoodItem>,
    simplifiedHome: Boolean = false,
    onOpenItem: (FoodItem) -> Unit,
    onGoNotif: () -> Unit,
    onGoRecipes: () -> Unit,
    onGoInv: () -> Unit,
    onOpenVoice: () -> Unit = {},           // ← 음성 어시스턴트 진입
    marketArrivals: List<FoodItem> = emptyList(),
    onDismissMarket: () -> Unit = {},
    onConfirmMarket: () -> Unit = {},
) {
    val criticalItems = inventory.filter { it.expiryStatus() in listOf(ExpiryStatus.CRITICAL, ExpiryStatus.EXPIRED) }

    if (simplifiedHome) {
        SimplifiedHomeScreen(colors, sizes, criticalItems, onOpenItem)
    } else {
        FullHomeScreen(
            colors, sizes, inventory, criticalItems, onOpenItem,
            onGoNotif, onGoRecipes, onGoInv, onOpenVoice,
            marketArrivals, onDismissMarket, onConfirmMarket,
        )
    }
}

// ── Simplified ────────────────────────────────────────────────────────────────

@Composable
private fun SimplifiedHomeScreen(
    colors: EatdaColors,
    sizes: EatdaSizes,
    criticalItems: List<FoodItem>,
    onOpenItem: (FoodItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.bg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (criticalItems.isEmpty()) colors.accentSoft else colors.dangerSoft)
                    .border(
                        2.dp,
                        if (criticalItems.isEmpty()) colors.accent.copy(alpha = 0.3f)
                        else colors.danger.copy(alpha = 0.4f),
                        RoundedCornerShape(16.dp),
                    )
                    .padding(24.dp)
                    .semantics {
                        contentDescription = if (criticalItems.isEmpty())
                            "오늘 확인할 식재료 없음"
                        else
                            "오늘 확인할 식재료 ${criticalItems.size}개"
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        if (criticalItems.isEmpty()) "오늘 확인할 식재료 없음"
                        else "오늘 확인할 식재료 ${criticalItems.size}개",
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = if (criticalItems.isEmpty()) colors.accentDeep else colors.danger,
                    )
                    if (criticalItems.isNotEmpty()) {
                        Text(
                            "아래 항목을 확인하고 사용하거나 폐기하세요",
                            fontSize = 13.sp,
                            color    = colors.danger.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }

        items(criticalItems) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .border(2.dp, colors.danger.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .clickable { onOpenItem(item) }
                    .padding(20.dp)
                    .semantics {
                        contentDescription = "${item.name}, ${item.qty}, ${item.formatExpiry()}, 긴급 확인 필요"
                    },
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(foodEmoji[item.name] ?: item.category.label.first().toString(), fontSize = 36.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.name,          fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = colors.text)
                    Text(item.formatExpiry(), fontSize = 15.sp, fontWeight = FontWeight.Bold,      color = colors.danger)
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

// ── Full ──────────────────────────────────────────────────────────────────────

@Composable
private fun FullHomeScreen(
    colors: EatdaColors,
    sizes: EatdaSizes,
    inventory: List<FoodItem>,
    criticalItems: List<FoodItem>,
    onOpenItem: (FoodItem) -> Unit,
    onGoNotif: () -> Unit,
    onGoRecipes: () -> Unit,
    onGoInv: () -> Unit,
    onOpenVoice: () -> Unit = {},
    marketArrivals: List<FoodItem> = emptyList(),
    onDismissMarket: () -> Unit = {},
    onConfirmMarket: () -> Unit = {},
) {
    val total        = inventory.size
    val warning      = inventory.count { it.expiryStatus() in listOf(ExpiryStatus.WARNING, ExpiryStatus.SOON) }
    val todayExp     = inventory.count { it.daysLeft().let { d -> d in 0..1 } }
    val expiredItems = inventory.filter { it.daysLeft() < 0 }

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    var ocrResult by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap == null) return@rememberLauncherForActivityResult
        scope.launch {
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
            val date = VisionService.extractExpiryDate(stream.toByteArray())
            ocrResult = date?.toString() ?: "유통기한 인식 실패"
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.bg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
    ) {
        // ── 인사말 + 제목 ─────────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 14.dp)) {
                Row(
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("좋은 저녁이에요", fontSize = 13.sp, color = colors.textMuted, fontWeight = FontWeight.Medium)
                    Text("👋", fontSize = 14.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    buildAnnotatedString {
                        append("길민재 님의 ")
                        pushStyle(SpanStyle(color = colors.accent))
                        append("냉장고")
                        pop()
                    },
                    fontSize   = if (sizes.fontBase >= 18) 25.sp else 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = colors.text,
                )
            }
        }

        // ── 통계 카드 ─────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard(colors, total,    "보관 식재료", StatCardTone(fg = colors.accent,     bg = colors.surfaceAlt), modifier = Modifier.weight(1f))
                StatCard(colors, warning,  "주의 필요",  StatCardTone(fg = colors.danger,     bg = colors.surfaceAlt), modifier = Modifier.weight(1f))
                StatCard(colors, todayExp, "오늘 만료",  StatCardTone(fg = Color(0xFF9B6B1F), bg = colors.surfaceAlt), modifier = Modifier.weight(1f))
            }
        }

        // ── 유통기한 만료 배너 ────────────────────────────────────────────────
        item { ExpiredBannerItem(colors, expiredItems, onGoInv, onOpenItem) }

        // ── 긴급 알림 ─────────────────────────────────────────────────────────
        item { SectionHeader(colors, sizes, "긴급 알림", "${criticalItems.size}건의 처리가 필요해요", onMore = onGoNotif) }

        items(criticalItems.take(3)) { item ->
            UrgentRow(colors, item, onClick = { onOpenItem(item) })
            Spacer(Modifier.height(8.dp))
        }

        item { Spacer(Modifier.height(6.dp)) }

        // ── 추천 레시피 ───────────────────────────────────────────────────────
        item { SectionHeader(colors, sizes, "오늘의 추천 레시피", "보유 재료 기반", onMore = onGoRecipes) }

        item {
            val recommended = recommendRecipes(inventory.map { it.name })
            recommended.firstOrNull()?.let { recipe ->
                RecipeHero(colors, recipe, onClick = onGoRecipes)
            }
        }

        // ── 싱싱마켓 배너 ─────────────────────────────────────────────────────
        item {
            val shopBrand     = Color(0xFF2F6DB5)
            val shopBrandSoft = Color(0xFFE4EDF7)
            val shopBrandDeep = Color(0xFF1E4E85)

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(shopBrandSoft, Color(0xFFD6E6F7))))
                    .border(1.dp, shopBrand.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .clickable {
                        val intent = Intent(context, com.eatda.app.ui.shop.ShopWebViewActivity::class.java)
                        context.startActivity(intent)
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(shopBrand),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🛒", fontSize = 20.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("싱싱마켓", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = shopBrandDeep)
                    Text(
                        "구매하면 냉장고 재고에 자동 추가",
                        fontSize = 11.sp,
                        color    = shopBrand.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(shopBrand)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text("바로가기 →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        item { Spacer(Modifier.height(28.dp)) }
    }

    // ── OCR 결과 다이얼로그 ───────────────────────────────────────────────────
    ocrResult?.let { result ->
        AlertDialog(
            onDismissRequest = { ocrResult = null },
            confirmButton    = { TextButton(onClick = { ocrResult = null }) { Text("확인") } },
            title = { Text("OCR 결과") },
            text  = { Text(result) },
        )
    }

    // ── 마켓 구매 도착 팝업 ───────────────────────────────────────────────────
    if (marketArrivals.isNotEmpty()) {
        MarketArrivalSheet(
            colors    = colors,
            items     = marketArrivals,
            onDismiss = onDismissMarket,
            onConfirm = onConfirmMarket,
        )
    }
}

// ── 컴포넌트 ──────────────────────────────────────────────────────────────────

@Composable
private fun UrgentRow(colors: EatdaColors, item: FoodItem, onClick: () -> Unit) {
    val status   = item.expiryStatus()
    val isDanger = status in listOf(ExpiryStatus.CRITICAL, ExpiryStatus.EXPIRED)
    val dotColor = if (isDanger) colors.danger else colors.warning
    val badgeBg  = if (isDanger) colors.dangerSoft else colors.warningSoft
    val badgeFg  = if (isDanger) colors.danger else Color(0xFF9B6B1F)
    val cat      = item.category
    val expiryText = if (colors.isColorBlind && isDanger) "⚠ ${item.formatExpiry()}" else item.formatExpiry()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .semantics {
                contentDescription = buildString {
                    append("${item.name}, ${item.qty}")
                    append(", ${item.formatExpiry()}")
                    if (item.isAllergen) append(", 알레르기 주의")
                }
            },
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        EatdaIcon(EatdaIcons.Warn, tint = dotColor, size = 18.dp)
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(cat.hexColor).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(foodEmoji[item.name] ?: item.name.first().toString(), fontSize = 18.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("${item.name} — ${item.formatExpiry()}", fontSize = 14.sp, fontWeight = FontWeight.Bold,   color = colors.text)
            Text(item.qty,                                fontSize = 11.sp, color = colors.textMuted, modifier = Modifier.padding(top = 1.dp))
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(badgeBg)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(expiryText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeFg)
        }
    }
}

@Composable
private fun RecipeHero(colors: EatdaColors, recipe: Recipe, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(Brush.linearGradient(listOf(colors.accentSoft, colors.surfaceAlt))),
            contentAlignment = Alignment.BottomStart,
        ) {
            Box(modifier = Modifier.fillMaxSize().background(colors.accentSoft.copy(alpha = 0.08f)))
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.surface)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                EatdaIcon(EatdaIcons.Sparkle, tint = colors.accent, size = 11.dp)
                Text(recipe.priority ?: recipe.tag, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.accent)
            }
        }
        Column(modifier = Modifier.padding(14.dp)) {
            Text("${recipe.minutes}분 · 보유 재료 ${recipe.ingredients.size}개", fontSize = 11.sp, color = colors.textMuted, modifier = Modifier.padding(top = 3.dp))
            Text(recipe.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.text)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                recipe.ingredients.forEach { ing ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.surfaceAlt)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(ing, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpiredBannerItem(
    colors: EatdaColors,
    items: List<FoodItem>,
    onMore: () -> Unit,
    onOpenItem: (FoodItem) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(bottom = 18.dp)) {
        if (items.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accentSoft)
                    .border(1.dp, colors.accent.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    EatdaIcon(EatdaIcons.Leaf, tint = colors.accent, size = 18.dp)
                }
                Text(
                    "유통기한이 지난 식품이 없어요",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = colors.accentDeep,
                    modifier   = Modifier.weight(1f),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.dangerSoft)
                    .border(1.dp, colors.danger.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(14.dp),
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.danger),
                        contentAlignment = Alignment.Center,
                    ) {
                        EatdaIcon(EatdaIcons.Warn, tint = Color.White, size = 20.dp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("유통기한 지난 식품 ${items.size}개", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = colors.danger)
                        Text("바로 확인하고 폐기 또는 출고하세요",  fontSize = 11.sp, color = colors.textMuted, modifier = Modifier.padding(top = 2.dp))
                    }
                    EatdaIcon(
                        if (expanded) EatdaIcons.ChevronDown else EatdaIcons.ChevronRight,
                        tint = colors.danger,
                        size = 18.dp,
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items.take(5).forEach { item ->
                            val cat = item.category
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                                    .clickable { onOpenItem(item) }
                                    .padding(10.dp)
                                    .semantics {
                                        contentDescription = "${item.name}, ${item.qty}, ${-item.daysLeft()}일 경과"
                                    },
                                verticalAlignment    = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(cat.hexColor).copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(foodEmoji[item.name] ?: item.name.first().toString(), fontSize = 16.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.Bold,  color = colors.text)
                                    Text(item.qty,  fontSize = 10.sp, color = colors.textFaint)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.danger)
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Text("${-item.daysLeft()}일 경과", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                        if (items.size > 5) {
                            Text(
                                "나머지 ${items.size - 5}개 모두 보기 ›",
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color      = colors.danger,
                                modifier   = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onMore)
                                    .padding(vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
