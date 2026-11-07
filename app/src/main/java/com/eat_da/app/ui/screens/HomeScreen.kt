package com.eatda.app.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.eatda.app.R
import com.eatda.app.data.*
import com.eatda.app.data.model.*
import com.eatda.app.ui.components.*
import com.eatda.app.ui.theme.*
import com.eatda.app.util.VisionService
import kotlinx.coroutines.launch
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.FlowRow

@Composable
fun HomeScreen(
    colors: EatdaColors,
    sizes: EatdaSizes,
    inventory: List<FoodItem>,
    recipes: List<Recipe>,
    simplifiedHome: Boolean = false,
    onOpenItem: (FoodItem) -> Unit,
    onGoNotif: () -> Unit,
    onGoImminent: () -> Unit,
    onGoRecipes: () -> Unit,
    onOpenRecipe: (Recipe) -> Unit,
    onGoInv: () -> Unit,
    onGoExpired: () -> Unit,
    onOpenVoice: () -> Unit = {},
    marketArrivals: List<FoodItem> = emptyList(),
    onDismissMarket: () -> Unit = {},
    onConfirmMarket: () -> Unit = {},
    onCheckDelivery: () -> Unit = {},
    onOpenInventoryGroup: (String) -> Unit,
) {
    val criticalItems = inventory.filter {
        it.expiryStatus() in listOf(ExpiryStatus.CRITICAL, ExpiryStatus.EXPIRED)
    }

    if (simplifiedHome) {
        SimplifiedHomeScreen(
            colors, sizes, criticalItems,
            onOpenInventoryGroup
        )
    } else {
        FullHomeScreen(
            colors, sizes, inventory, recipes, criticalItems, onOpenItem,
            onGoNotif, onGoImminent, onGoRecipes, onOpenRecipe, onGoInv, onGoExpired, onOpenVoice,
            marketArrivals, onDismissMarket, onConfirmMarket, onCheckDelivery,
        )
    }
}

// ── Simplified ────────────────────────────────────────────────────────────────

@Composable
private fun SimplifiedHomeScreen(
    colors: EatdaColors,
    sizes: EatdaSizes,
    criticalItems: List<FoodItem>,
    onOpenInventoryGroup: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(
            if (colors.isColorBlind) colors.bg else Color.White
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (colors.isColorBlind) {
                            if (criticalItems.isEmpty()) colors.accentSoft else colors.dangerSoft
                        } else {
                            if (criticalItems.isEmpty()) Color(0xFFDCEBDD) else Color(0xFFF9F0EE)
                        }
                    )
                    .border(
                        1.dp,
                        if (colors.isColorBlind) {
                            if (criticalItems.isEmpty()) colors.accent.copy(alpha = 0.3f)
                            else colors.danger.copy(alpha = 0.4f)
                        } else {
                            if (criticalItems.isEmpty()) Color(0xFFD1E3D4) else Color(0xFFEBCFCA)
                        },
                        RoundedCornerShape(18.dp),
                    )
                    .padding(horizontal = 18.dp, vertical = 24.dp)
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
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (criticalItems.isEmpty()) colors.accentDeep else colors.danger,
                    )
                    if (criticalItems.isNotEmpty()) {
                        Text(
                            "아래 항목을 확인하고 사용하거나 폐기하세요",
                            fontSize = 15.sp,
                            color = if (colors.isColorBlind) colors.danger.copy(alpha = 0.8f) else Color(0xFF99564D),
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
                    .background(if (colors.isColorBlind) colors.surface else Color.White)
                    .border(
                        1.dp,
                        if (colors.isColorBlind) colors.danger.copy(alpha = 0.5f) else Color(0xFFE2E7E2),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onOpenInventoryGroup(item.name) }
                    .padding(20.dp)
                    .semantics {
                        contentDescription = "${item.name}, ${item.qty}, ${item.formatExpiry()}, 긴급 확인 필요"
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier.size(66.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (colors.isColorBlind) colors.surfaceAlt else Color(0xFFF1F4F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(foodEmoji[item.name] ?: item.category.label.first().toString(), fontSize = 36.sp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        item.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (colors.isColorBlind) colors.text else Color(0xFF242C27)
                    )
                    Text(
                        item.formatExpiry(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.danger,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (colors.isColorBlind) colors.dangerSoft else Color(0xFFF9E9E7))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
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
    recipes: List<Recipe>,
    criticalItems: List<FoodItem>,
    onOpenItem: (FoodItem) -> Unit,
    onGoNotif: () -> Unit,
    onGoImminent: () -> Unit,
    onGoRecipes: () -> Unit,
    onOpenRecipe: (Recipe) -> Unit,
    onGoInv: () -> Unit,
    onGoExpired: () -> Unit,
    onOpenVoice: () -> Unit = {},
    marketArrivals: List<FoodItem> = emptyList(),
    onDismissMarket: () -> Unit = {},
    onConfirmMarket: () -> Unit = {},
    onCheckDelivery: () -> Unit = {},
) {
    // 홈 상단 요약: 오늘 만료 / 임박 / 주의 식재료
    val todayExp = inventory
        .filter { it.daysLeft() == 0 }
        .distinctBy { it.name }
        .size

    val expiredItems = inventory
        .filter { it.expiryStatus() == ExpiryStatus.EXPIRED }
        .distinctBy { it.name }

    val imminentItems = inventory
        .filter { it.daysLeft() in 0..3 }
        .distinctBy { it.name }

    val cautionItems = inventory
        .filter { it.daysLeft() in 4..7 }
        .distinctBy { it.name }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var ocrResult by remember { mutableStateOf<String?>(null) }
    var sheetOpen by remember { mutableStateOf(false) }
    var urgentExpanded by remember { mutableStateOf(false) }

    val shopLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { onCheckDelivery() }

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

    Column(modifier = Modifier.fillMaxSize().background(if (colors.isColorBlind) colors.bg else Color.White)) {
        if (marketArrivals.isNotEmpty() && !sheetOpen) {
            DeliveryArrivalBanner(
                count = marketArrivals.size,
                colors = colors,
                onClick = { sheetOpen = true },
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).background(if (colors.isColorBlind) colors.bg else Color.White),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        ) {

            // ── Figma: 인사말 (기존 데이터 및 콜백 변경 없음) ──
            item {
                Column(Modifier.fillMaxWidth().padding(top = 17.dp, bottom = 7.dp)) {
                    Text(
                        "길민재 님",
                        fontSize = if (sizes.fontBase >= 18) 28.sp else 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = homeInk(colors)
                    )
                }
            }

            // ── Figma: 오늘 만료 / 임박 / 주의 식재료 현황 ──
            item {
                Column(Modifier.fillMaxWidth().padding(bottom = 19.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (colors.isColorBlind) colors.accentSoft else Color(0xFFDCEBDD))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text("오늘 만료", fontSize = 13.sp, color = homeMuted(colors))
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "$todayExp",
                                fontSize = 40.sp,
                                lineHeight = 48.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = homeGreen(colors)
                            )
                            Text(
                                "개",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = homeGreen(colors),
                                modifier = Modifier.padding(bottom = 7.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(15.dp))
                    Row(Modifier.fillMaxWidth().height(60.dp)) {
                        Column(Modifier.weight(1f).padding(start = 2.dp)) {
                            Text("임박 식재료", fontSize = 13.sp, color = colors.danger)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${imminentItems.size}개",
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.danger
                            )
                        }
                        Box(Modifier.width(1.dp).fillMaxHeight().background(Color(0xFFF0F2EF)))
                        Column(Modifier.weight(1f).padding(start = 19.dp)) {
                            Text("주의 식재료", fontSize = 13.sp, color = colors.warning)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${cautionItems.size}개",
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.warning
                            )
                        }
                    }
                }
            }

            // ── 소비기한 임박 배너 ────────────────────────────────────────────────
            item { ExpiredBannerItem(colors, imminentItems, onGoImminent, onOpenItem) }

            // ── 소비기한 경과 식재료: 접기/펼치기 ───────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF3E9E6))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { urgentExpanded = !urgentExpanded }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(Modifier.width(14.dp), contentAlignment = Alignment.Center) {
                            EatdaIcon(EatdaIcons.Leaf, tint = Color(0xFF795548), size = 19.dp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "소비기한이 경과된 식재료 ${expiredItems.size}개",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF795548)
                            )
                            Text(
                                "눌러서 식재료 목록을 확인하세요",
                                fontSize = 12.sp,
                                color = homeMuted(colors),
                                modifier = Modifier.padding(top = 5.dp)
                            )
                        }
                        EatdaIcon(
                            if (urgentExpanded) EatdaIcons.ChevronDown else EatdaIcons.ChevronRight,
                            tint = Color(0xFF795548),
                            size = 18.dp
                        )
                    }
                    AnimatedVisibility(visible = urgentExpanded) {
                        Column(
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            expiredItems.take(3).forEach { item ->
                                UrgentRow(colors, item, onClick = { onOpenItem(item) })
                            }
                            if (expiredItems.isNotEmpty()) {
                                Text(
                                    "전체보기 →",
                                    fontSize = 12.sp,
                                    color = Color(0xFF795548),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = onGoExpired)
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── 추천 레시피 ───────────────────────────────────────────────────────
            item {
                Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "오늘의 추천 레시피",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = homeInk(colors),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "전체보기 →",
                            fontSize = 12.sp,
                            color = homeLink(colors),
                            modifier = Modifier.clickable(onClick = onGoRecipes)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("보유 재료 기반", fontSize = 12.sp, color = homeMuted(colors))
                }
            }

            item {
                val recommended = recommendRecipes(
                    inventory.map { it.name },
                    recipes = recipes,
                    inventory = inventory
                )

                recommended.firstOrNull()?.let { recipe ->

                    // RecipesScreen과 동일한 재료 매칭 기준 적용
                    val ingredientQuantityRegex = Regex(
                        """\(?\d+(?:\.\d+)?(?:/\d+)?\s*(?:kg|g|mg|ml|l|개|모|팩|봉|장|대|통|구|병|캔|컵|인분)\)?$"""
                    )
                    fun normalizeIngredient(name: String): String = name
                        .trim()
                        .replace(" ", "")
                        .replace("　", "")
                        .replace(ingredientQuantityRegex, "")
                        .trim()

                    val matchedIngredientCount = recipe.ingredients.distinct().count { ingredient ->
                        val recipeName = normalizeIngredient(ingredient)
                        recipeName.isNotBlank() && inventory.any { item ->
                            item.expiryStatus() != ExpiryStatus.EXPIRED &&
                                    recipeName == normalizeIngredient(item.name)
                        }
                    }

                    RecipeHero(
                        colors = colors,
                        recipe = recipe,
                        matchedIngredientCount = matchedIngredientCount,
                        onClick = {
                            onOpenRecipe(recipe)
                        }
                    )
                }
            }

            // ── 싱싱마켓 배너 ─────────────────────────────────────────────────────
            item {
                val shopBrand = Color(0xFF35546A)
                val shopBrandSoft = Color(0xFFD7E3EF)
                val shopBrandDeep = Color(0xFF283945)

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (colors.isColorBlind) colors.accentSoft else shopBrandSoft)
                        .border(
                            1.dp,
                            if (colors.isColorBlind) colors.border else Color(0xFFCBDBE9),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            shopLauncher.launch(
                                Intent(context, com.eatda.app.ui.shop.ShopWebViewActivity::class.java)
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color(0xFFC6D9E8)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🛒", fontSize = 20.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("싱싱마켓", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = shopBrandDeep)
                        Text(
                            "구매하면 냉장고 재고에 자동 추가",
                            fontSize = 11.sp,
                            color = shopBrand.copy(alpha = 0.7f),
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

            // ── 신선도 낮은 식품 구매 유도 ────────────────────────────────────────
            item {
                val nudgeItem = inventory
                    .filter { it.daysLeft() < 0 }
                    .maxByOrNull { it.daysLeft() }
                nudgeItem?.let {
                    FreshnessNudgeBubble(colors = colors, item = it, onOpenShop = {
                        shopLauncher.launch(Intent(context, com.eatda.app.ui.shop.ShopWebViewActivity::class.java))
                    })
                    Spacer(Modifier.height(4.dp))
                }
            }

            item { Spacer(Modifier.height(28.dp)) }
        }
    }

    // ── OCR 결과 다이얼로그 ───────────────────────────────────────────────────
    ocrResult?.let { result ->
        AlertDialog(
            onDismissRequest = { ocrResult = null },
            confirmButton = { TextButton(onClick = { ocrResult = null }) { Text("확인") } },
            title = { Text("OCR 결과") },
            text = { Text(result) },
        )
    }

    // ── 마켓 구매 도착 팝업 ───────────────────────────────────────────────────
    if (sheetOpen) {
        MarketArrivalSheet(
            colors = colors,
            items = marketArrivals,
            onDismiss = { sheetOpen = false; onDismissMarket() },
            onConfirm = { sheetOpen = false; onConfirmMarket() },
        )
    }
}

// ── 컴포넌트 ──────────────────────────────────────────────────────────────────

@Composable
private fun UrgentRow(colors: EatdaColors, item: FoodItem, onClick: () -> Unit) {
    val status = item.expiryStatus()
    val isDanger = status in listOf(ExpiryStatus.CRITICAL, ExpiryStatus.EXPIRED)
    val badgeTone = homeExpiryBadgeTone(colors, status)
    val badgeBg = badgeTone.first
    val badgeFg = badgeTone.second
    val cat = item.category
    val expiryText = if (colors.isColorBlind && isDanger) "⚠ ${item.formatExpiry()}" else item.formatExpiry()

    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (colors.isColorBlind) colors.surface else Color.White)
            .border(
                1.dp,
                if (colors.isColorBlind) colors.border else Color(0xFFE2E7E2),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics {
                contentDescription = buildString {
                    append("${item.name}, ${item.qty}")
                    append(", ${item.formatExpiry()}")
                    if (item.isAllergen) append(", 알레르기 주의")
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                .background(if (colors.isColorBlind) colors.surfaceAlt else Color(0xFFF1F4F0)),
            contentAlignment = Alignment.Center
        ) {
            Text(foodEmoji[item.name] ?: item.name.first().toString(), fontSize = 18.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = homeInk(colors))
            Text(
                "${item.qty} · ${item.expiry.toString().replace("-", ".")}",
                fontSize = 12.sp,
                color = if (colors.isColorBlind) colors.textMuted else Color(0xFF596B61)
            )
        }
        Box(
            Modifier.clip(RoundedCornerShape(9.dp))
                .background(badgeBg)
                .padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            Text(
                expiryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = badgeFg,
            )
        }
    }
}

@Composable
private fun RecipeHero(
    colors: EatdaColors,
    recipe: Recipe,
    matchedIngredientCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 155.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(
                1.dp,
                if (colors.isColorBlind) colors.border else Color(0xFFE2E7E2),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.width(149.dp).height(155.dp)
            .background(if (colors.isColorBlind) colors.surfaceAlt else Color(0xFFE4E8E2))) {
            AsyncImage(
                model = recipe.imageUrl,
                contentDescription = recipe.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(Modifier.align(Alignment.BottomStart).padding(9.dp)
                .clip(RoundedCornerShape(5.dp)).background(colors.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text(
                    recipe.priority ?: recipe.tag,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = homeLink(colors)
                )
            }
        }
        Column(
            Modifier.weight(1f).padding(start = 12.dp, end = 8.dp, top = 20.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(recipe.title, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                color = homeInk(colors), maxLines = 2)
            Text("${recipe.minutes}분 · 보유 재료 ${matchedIngredientCount}개",
                fontSize = 12.sp, color = homeMuted(colors))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                maxLines = 2
            ) {
                recipe.ingredients.forEach { ing ->
                    Text(
                        ing,
                        fontSize = 11.sp,
                        color = if (colors.isColorBlind) colors.accentDeep else Color(0xFF496953),
                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(if (colors.isColorBlind) colors.accentSoft else Color(0xFFEDF3ED))
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                    )
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
                verticalAlignment = Alignment.CenterVertically,
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
                    "소비기한이 임박한 식품이 없어요",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accentDeep,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (colors.isColorBlind) colors.dangerSoft else Color(0xFFF9F3F1)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.width(14.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "!",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (colors.isColorBlind) colors.danger else Color(0xFFA34D42)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "소비기한이 임박한 식품 ${items.size}개",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (colors.isColorBlind) colors.danger else Color(0xFF93493E)
                        )
                        Text(
                            "지금 확인하고 정리해 주세요",
                            fontSize = 12.sp,
                            color = if (colors.isColorBlind) colors.textMuted else Color(0xFF77706D),
                            modifier = Modifier.padding(top = 5.dp)
                        )
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
                                        contentDescription = "${item.name}, ${item.qty}, ${item.formatExpiry()}"
                                    },
                                verticalAlignment = Alignment.CenterVertically,
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
                                    Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text)
                                    Text(item.qty, fontSize = 10.sp, color = colors.textFaint)
                                }
                                val expiryTone = homeExpiryBadgeTone(colors, item.expiryStatus())
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(expiryTone.first)
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        item.formatExpiry(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = expiryTone.second,
                                    )
                                }
                            }
                        }
                        if (items.size > 5) {
                            Text(
                                "나머지 ${items.size - 5}개 모두 보기 ›",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.danger,
                                modifier = Modifier
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

@Composable
private fun DeliveryArrivalBanner(count: Int, colors: EatdaColors, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2EC57E))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("📦", fontSize = 18.sp)
        Text(
            "싱싱마켓 배송 도착 · ${count}개",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Text(
            "확인하기 →",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun FreshnessNudgeBubble(colors: EatdaColors, item: FoodItem, onOpenShop: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "nudge")
    val nudgeAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "nudge_alpha",
    )

    val bubbleColor = Color(0xFFFFF3CD)
    val bubbleBorder = Color(0xFFE8C96B)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-14).dp)
            .alpha(nudgeAlpha)
            .clickable { onOpenShop() },
    ) {
        Canvas(
            modifier = Modifier
                .padding(start = 20.dp)
                .size(18.dp, 10.dp),
        ) {
            val outer = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(outer, color = bubbleBorder.copy(alpha = 0.7f))
            val inner = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width / 2f, 1.5f)
                lineTo(size.width - 1f, size.height)
                lineTo(1f, size.height)
                close()
            }
            drawPath(inner, color = bubbleColor)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(bubbleColor)
                .border(1.dp, bubbleBorder.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(foodEmoji[item.name] ?: "🍽", fontSize = 20.sp, lineHeight = 20.sp)
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    "${item.name} 유통기한이 지났어요",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3D2B00),
                )
                Text(
                    "싱싱마켓 새벽배송으로 지금 주문하세요",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF7A5C00),
                )
            }
        }
    }
}

// 재고 목록의 경과 / 임박 / 주의 카테고리와 동일한 소비기한 배지 색상
private fun homeExpiryBadgeTone(colors: EatdaColors, status: ExpiryStatus): Pair<Color, Color> =
    when (status) {
        ExpiryStatus.EXPIRED ->
            Pair(Color(0xFFF3E9E6), Color(0xFF795548))
        ExpiryStatus.CRITICAL,
        ExpiryStatus.WARNING ->
            Pair(colors.dangerSoft, colors.danger)
        ExpiryStatus.SOON ->
            Pair(colors.warningSoft, Color(0xFF9B6B1F))
        ExpiryStatus.FRESH ->
            Pair(colors.accentSoft, colors.accentDeep)
    }

private val EatdaColors.isColorBlind: Boolean get() = this == ColorBlindEatdaColors

// Figma palette used only by the normal home UI. Color-accessibility mode keeps theme colors.
private fun homeInk(colors: EatdaColors) = if (colors.isColorBlind) colors.text else Color(0xFF242C27)
private fun homeMuted(colors: EatdaColors) = if (colors.isColorBlind) colors.textMuted else Color(0xFF65736B)
private fun homeGreen(colors: EatdaColors) = if (colors.isColorBlind) colors.accentDeep else Color(0xFF254D39)
private fun homeLink(colors: EatdaColors) = if (colors.isColorBlind) colors.accent else Color(0xFF28794A)