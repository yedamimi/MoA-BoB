package com.eatda.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eatda.app.data.*
import com.eatda.app.data.model.*
import com.eatda.app.ui.components.*
import com.eatda.app.ui.theme.*
import androidx.activity.compose.BackHandler
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Surface

private enum class InvFilter(val label: String) {
    ALL("전체"),
    EXPIRED("경과"),
    CRITICAL("임박"),
    WARNING("주의"),
    VEGETABLE("채소"),
    MEAT("육류"),
    DAIRY("유제품"),
    FRUIT("과일"),
    SEAFOOD("해산물"),
    PROCESSED("가공식품"),
}

// 화면 표시용 수량 숫자 추출
// 예: "2개" -> 2, "2" -> 2
private fun extractInventoryQty(qty: String): Int {
    return Regex("""\d+""")
        .find(qty)
        ?.value
        ?.toIntOrNull()
        ?: 0
}

@Composable
fun InventoryScreen(
    colors: EatdaColors,
    sizes: EatdaSizes,
    inventory: List<FoodItem>,
    onOpenItem: (FoodItem) -> Unit,
    onRecipeRecommend: (String) -> Unit,
    ) {
    var filter by remember { mutableStateOf(InvFilter.ALL) }
    var query by remember { mutableStateOf("") }

    // null이면 기존 재고 목록,
    // 값이 있으면 해당 식재료의 유통기한별 재고 화면
    var selectedFoodName by remember {
        mutableStateOf<String?>(null)
    }

    BackHandler(enabled = selectedFoodName != null) {
        selectedFoodName = null
    }

    // 식재료별 재고 화면
    selectedFoodName?.let { foodName ->
        InventoryGroupView(
            colors = colors,
            foodName = foodName,
            inventory = inventory,
            onBack = {
                selectedFoodName = null
            },
            onOpenItem = onOpenItem,
            onRecipeRecommend = onRecipeRecommend,
        )

        return
    }

    // 유통기한 경과 전용 색상
    // 경과 = 차분한 갈색 계열
    // 임박 = 기존 빨강 계열
    // 주의 = 기존 노랑/주황 계열
    val expiredColor = Color(0xFF795548)
    val expiredSoft = Color(0xFFF3E9E6)

    val filtered = inventory
        .filter { item ->
            when (filter) {
                InvFilter.ALL -> true

                InvFilter.EXPIRED ->
                    item.expiryStatus() == ExpiryStatus.EXPIRED

                InvFilter.CRITICAL ->
                    item.expiryStatus() == ExpiryStatus.CRITICAL

                InvFilter.WARNING -> item.expiryStatus() in listOf(
                    ExpiryStatus.WARNING,
                    ExpiryStatus.SOON
                )

                InvFilter.VEGETABLE ->
                    item.category == FoodCategory.VEGETABLE

                InvFilter.MEAT ->
                    item.category == FoodCategory.MEAT

                InvFilter.DAIRY ->
                    item.category == FoodCategory.DAIRY

                InvFilter.FRUIT ->
                    item.category == FoodCategory.FRUIT

                InvFilter.SEAFOOD ->
                    item.category == FoodCategory.SEAFOOD

                InvFilter.PROCESSED ->
                    item.category == FoodCategory.PROCESSED
            }
        }
        .filter {
            if (query.isBlank()) {
                true
            } else {
                it.name.contains(query)
            }
        }
        .sortedBy { it.daysLeft() }

    // 필터링된 재고를 name 기준으로 그룹화
    // Firebase 원본 데이터는 변경하지 않고 화면에서만 묶음
    val groupedInventory = filtered
        .groupBy { it.name }
        .map { (_, items) ->

            // 같은 이름의 재고 중 소비기한이 가장 빠른 항목을 대표로 사용
            val representative = items.minByOrNull { it.expiry }
                ?: items.first()

            // 같은 이름의 재고 수량 합산
            val totalQty = items.sumOf {
                extractInventoryQty(it.qty)
            }

            representative.copy(
                qty = "${totalQty}개",

                // 같은 이름 중 하나라도 알레르기라면 대표 카드에 표시
                isAllergen = items.any { it.isAllergen }
            )
        }
        .sortedBy { it.daysLeft() }

    val expiredCount = inventory
        .filter {
            it.expiryStatus() == ExpiryStatus.EXPIRED
        }
        .distinctBy {
            it.name
        }
        .size

    val criticalCount = inventory
        .filter {
            it.expiryStatus() == ExpiryStatus.CRITICAL
        }
        .distinctBy {
            it.name
        }
        .size

    val warningCount = inventory
        .filter {
            it.expiryStatus() in listOf(
                ExpiryStatus.WARNING,
                ExpiryStatus.SOON
            )
        }
        .distinctBy {
            it.name
        }
        .size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {

        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {

            Text(
                "재고 목록",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.text,
                modifier = Modifier.padding(vertical = 10.dp)
            )

            // 검색창
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(
                        1.dp,
                        colors.border,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {

                EatdaIcon(
                    EatdaIcons.Search,
                    tint = colors.textMuted,
                    size = 18.dp
                )

                BasicTextField(
                    value = query,
                    onValueChange = {
                        query = it
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = colors.text
                    ),
                    decorationBox = { innerTextField ->

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {

                            if (query.isEmpty()) {
                                Text(
                                    "재고 검색",
                                    fontSize = 14.sp,
                                    color = colors.textFaint
                                )
                            }

                            innerTextField()
                        }
                    }
                )
            }
        }

        // 카테고리 필터
        LazyRow(
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {

            items(InvFilter.entries) { f ->

                val active = filter == f

                val count = when (f) {
                    InvFilter.ALL ->
                        inventory.distinctBy { it.name }.size

                    InvFilter.EXPIRED ->
                        expiredCount

                    InvFilter.CRITICAL ->
                        criticalCount

                    InvFilter.WARNING ->
                        warningCount

                    else ->
                        null
                }

                // 필터별 대표 색상
                val chipAccent = when (f) {
                    InvFilter.EXPIRED ->
                        expiredColor

                    InvFilter.CRITICAL ->
                        colors.danger

                    InvFilter.WARNING ->
                        colors.warning

                    else ->
                        colors.accent
                }

                // 선택됐을 때 진한 색,
                // 선택되지 않았을 때 연한 색
                val chipBg = when {
                    active && f == InvFilter.EXPIRED ->
                        expiredColor

                    active && f == InvFilter.CRITICAL ->
                        colors.danger

                    active && f == InvFilter.WARNING ->
                        colors.warning

                    active ->
                        colors.accent

                    f == InvFilter.EXPIRED ->
                        expiredSoft

                    f == InvFilter.CRITICAL ->
                        colors.dangerSoft

                    f == InvFilter.WARNING ->
                        colors.warningSoft

                    else ->
                        colors.surface
                }

                val chipBorder = when {
                    active ->
                        chipAccent

                    f == InvFilter.EXPIRED ->
                        expiredColor.copy(alpha = 0.4f)

                    f == InvFilter.CRITICAL ->
                        colors.danger.copy(alpha = 0.4f)

                    f == InvFilter.WARNING ->
                        colors.warning.copy(alpha = 0.4f)

                    else ->
                        colors.border
                }

                val chipText = when {
                    active ->
                        Color.White

                    f == InvFilter.EXPIRED ->
                        expiredColor

                    f == InvFilter.CRITICAL ->
                        colors.danger

                    f == InvFilter.WARNING ->
                        Color(0xFF9B6B1F)

                    else ->
                        colors.text
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(chipBg)
                        .border(
                            if (active) 1.5.dp else 1.dp,
                            chipBorder,
                            RoundedCornerShape(999.dp)
                        )
                        .clickable {
                            filter = f
                        }
                        .padding(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),
                ) {

                    Text(
                        if (count != null) {
                            "${f.label} $count"
                        } else {
                            f.label
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = chipText,
                    )
                }
            }
        }

        // 재고 목록
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 4.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            items(
                items = groupedInventory,
                key = { it.name }
            ) { item ->

                InventoryRow(
                    colors = colors,
                    item = item,
                    onClick = {
                        // 기존처럼 바로 상세 시트를 열지 않고
                        // 같은 이름의 재고 목록 화면으로 이동
                        selectedFoodName = item.name
                    }
                )
            }

            item {
                Spacer(
                    Modifier.height(20.dp)
                )
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────
// 식재료별 유통기한 재고 화면
//
// 예:
// 연어
// 총 3개
//
// 2026.09.30   2개
// 2026.10.28   1개
//
// 개별 카드를 누르면 기존 ItemDetailSheet가 열림
// ─────────────────────────────────────────────────────────────

@Composable
private fun InventoryGroupView(
    colors: EatdaColors,
    foodName: String,
    inventory: List<FoodItem>,
    onBack: () -> Unit,
    onOpenItem: (FoodItem) -> Unit,
    onRecipeRecommend: (String) -> Unit,
) {
    var showStorageGuide by remember {
        mutableStateOf(false)
    }

    // 같은 이름의 원본 FoodItem만 가져옴
    // 여기서는 그룹화하지 않음.
    // Firebase의 개별 재고 단위를 그대로 보여줘야 하기 때문.
    val foodItems = inventory
        .filter {
            it.name == foodName
        }
        .sortedBy {
            it.expiry
        }

    val totalQty = foodItems.sumOf {
        extractInventoryQty(it.qty)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {

        // ─────────────────────────────
        // 상단
        // ─────────────────────────────

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onBack()
                    },
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "‹",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.text
                )
            }

            Spacer(
                Modifier.width(4.dp)
            )

            Text(
                text = foodName,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.text
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ─────────────────────────────
            // 식재료 요약
            // ─────────────────────────────

            item {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 6.dp,
                            bottom = 14.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    val representative =
                        foodItems.firstOrNull()

                    val categoryColor =
                        representative?.category?.hexColor
                            ?: FoodCategory.GRAIN.hexColor

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(
                                RoundedCornerShape(16.dp)
                            )
                            .background(
                                Color(categoryColor)
                                    .copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = foodEmoji[foodName]
                                ?: foodName.firstOrNull()
                                    ?.toString()
                                ?: "?",
                            fontSize = 30.sp
                        )
                    }

                    Spacer(
                        Modifier.width(14.dp)
                    )

                    Column {

                        Text(
                            text = foodName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.text
                        )

                        Spacer(
                            Modifier.height(4.dp)
                        )

                        Text(
                            text = "총 ${totalQty}개",
                            fontSize = 14.sp,
                            color = colors.textMuted
                        )
                    }
                }
            }

            // ─────────────────────────────
// 맞춤 보관 가이드
// ─────────────────────────────

            item {
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
                                size = 16.dp
                            )

                            Text(
                                text = "맞춤 보관 가이드",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accentDeep
                            )
                        }

                        Text(
                            text = "자세히 보기  ›",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accentDeep
                        )
                    }

                    Text(
                        text = storageGuideSummary(foodName),
                        fontSize = 13.sp,
                        color = colors.text
                    )
                }
            }

            // ─────────────────────────────
// 레시피 추천
// ─────────────────────────────

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface)
                        .border(
                            1.dp,
                            colors.border,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            onRecipeRecommend(foodName)
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.accentSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        EatdaIcon(
                            EatdaIcons.Sparkle,
                            tint = colors.accentDeep,
                            size = 18.dp
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = "$foodName 레시피 추천",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )

                        Text(
                            text = "보유 재료를 활용한 레시피를 확인해보세요.",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }

                    Text(
                        text = "추천 보기  ›",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentDeep
                    )
                }
            }

            // ─────────────────────────────
            // 유통기한별 재고 제목
            // ─────────────────────────────

            item {

                Text(
                    text = "유통기한별 재고 (${foodItems.size}개)",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.text,
                    modifier = Modifier.padding(
                        top = 4.dp,
                        bottom = 2.dp
                    )
                )
            }

            // ─────────────────────────────
            // 실제 Firebase 개별 재고
            // ─────────────────────────────

            items(
                items = foodItems,
                key = { it.id }
            ) { item ->

                InventoryGroupItemRow(
                    colors = colors,
                    item = item,
                    onClick = {
                        // 중요:
                        // 그룹화된 대표 FoodItem이 아니라
                        // Firebase의 원본 FoodItem을 넘김.
                        //
                        // 따라서 기존 ItemDetailSheet에서
                        // 정확한 Firebase key를 기준으로 수정 가능.
                        onOpenItem(item)
                    }
                )
            }

            item {
                Spacer(
                    Modifier.height(20.dp)
                )
            }
        }
    }
    // ─────────────────────────────
    // 맞춤 보관 가이드 팝업
    // ─────────────────────────────
    if (showStorageGuide) {
        Dialog(
            onDismissRequest = {
                showStorageGuide = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .width(320.dp)
                        .heightIn(
                            min = 300.dp,
                            max = 420.dp
                        )
                        .border(
                            1.dp,
                            colors.border,
                            RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.bg,
                    shadowElevation = 12.dp,
                    tonalElevation = 6.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
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
                                    size = 20.dp
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "맞춤 보관 가이드",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.text
                                )

                                Text(
                                    "${foodEmoji[foodName] ?: ""} $foodName",
                                    fontSize = 12.sp,
                                    color = colors.textMuted,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        HorizontalDivider(
                            color = colors.border
                        )

                        Spacer(Modifier.height(16.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                storageGuide(foodName),
                                fontSize = 14.sp,
                                lineHeight = 23.sp,
                                color = colors.text
                            )
                        }

                        Spacer(Modifier.height(16.dp))

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
                                "확인",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────
// 식재료별 화면의 개별 재고 카드
// ─────────────────────────────────────────────────────────────

@Composable
private fun InventoryGroupItemRow(
    colors: EatdaColors,
    item: FoodItem,
    onClick: () -> Unit,
) {

    val status = item.expiryStatus()

    val tone = when (status) {

        ExpiryStatus.EXPIRED ->
            Pair(
                Color(0xFFF3E9E6),
                Color(0xFF795548)
            )

        ExpiryStatus.CRITICAL ->
            Pair(
                colors.dangerSoft,
                colors.danger
            )

        ExpiryStatus.WARNING,
        ExpiryStatus.SOON ->
            Pair(
                colors.warningSoft,
                Color(0xFF9B6B1F)
            )

        ExpiryStatus.FRESH ->
            Pair(
                colors.accentSoft,
                colors.accentDeep
            )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(14.dp)
            )
            .background(
                colors.surface
            )
            .border(
                1.dp,
                colors.border,
                RoundedCornerShape(14.dp)
            )
            .clickable(
                onClick = onClick
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(
                    RoundedCornerShape(12.dp)
                )
                .background(
                    Color(item.category.hexColor)
                        .copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = foodEmoji[item.name]
                    ?: item.name.firstOrNull()
                        ?.toString()
                    ?: "?",
                fontSize = 23.sp
            )
        }

        Spacer(
            Modifier.width(12.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = item.expiry
                    .toString()
                    .replace("-", "."),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text
            )

            Spacer(
                Modifier.height(4.dp)
            )

            Text(
                text = item.qty,
                fontSize = 13.sp,
                color = colors.textMuted
            )
        }

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(6.dp)
                )
                .background(
                    tone.first
                )
                .padding(
                    horizontal = 8.dp,
                    vertical = 4.dp
                )
        ) {

            Text(
                text = item.formatExpiry(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = tone.second
            )
        }
    }
}


// ─────────────────────────────────────────────────────────────
// 기존 메인 재고 카드
// ─────────────────────────────────────────────────────────────

@Composable
fun InventoryRow(
    colors: EatdaColors,
    item: FoodItem,
    onClick: () -> Unit
) {

    val status = item.expiryStatus()

    val tone = when (status) {

        ExpiryStatus.EXPIRED,
        ExpiryStatus.CRITICAL ->
            Pair(
                colors.dangerSoft,
                colors.danger
            )

        ExpiryStatus.WARNING,
        ExpiryStatus.SOON ->
            Pair(
                colors.warningSoft,
                Color(0xFF9B6B1F)
            )

        ExpiryStatus.FRESH ->
            Pair(
                colors.accentSoft,
                colors.accentDeep
            )
    }

    val cat = item.category

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(14.dp)
            )
            .background(
                colors.surface
            )
            .border(
                1.dp,
                colors.border,
                RoundedCornerShape(14.dp)
            )
            .clickable(
                onClick = onClick
            )
            .padding(
                12.dp,
                12.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(
                    RoundedCornerShape(12.dp)
                )
                .background(
                    Color(cat.hexColor)
                        .copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center,
        ) {

            Text(
                foodEmoji[item.name]
                    ?: item.name.first().toString(),
                fontSize = 22.sp
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                Text(
                    item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )

                if (item.isAllergen) {

                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(4.dp)
                            )
                            .background(
                                colors.dangerSoft
                            )
                            .padding(
                                horizontal = 5.dp,
                                vertical = 2.dp
                            ),
                    ) {

                        Text(
                            "알레르기",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.danger
                        )
                    }
                }
            }

            Text(
                item.qty,
                fontSize = 11.sp,
                color = colors.textMuted,
                modifier = Modifier.padding(
                    top = 2.dp
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(6.dp)
                    )
                    .background(
                        tone.first
                    )
                    .padding(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    ),
            ) {

                Text(
                    item.formatExpiry(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = tone.second
                )
            }

            if (item.freshness != null) {

                Text(
                    "신선도 ${item.freshness}%",
                    fontSize = 10.sp,
                    color = colors.textFaint,
                )
            }
        }
    }
}