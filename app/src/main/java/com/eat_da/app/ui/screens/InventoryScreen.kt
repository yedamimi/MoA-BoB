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

private enum class InvFilter(val label: String) {
    ALL("전체"), CRITICAL("임박"), WARNING("주의"),
    VEGETABLE("채소"), MEAT("육류"), DAIRY("유제품"),
    FRUIT("과일"), SEAFOOD("해산물"),
}

@Composable
fun InventoryScreen(
    colors: EatdaColors,
    sizes: EatdaSizes,
    inventory: List<FoodItem>,
    onOpenItem: (FoodItem) -> Unit,
) {
    var filter by remember { mutableStateOf(InvFilter.ALL) }
    var query by remember { mutableStateOf("") }

    val filtered = inventory
        .filter { item ->
            when (filter) {
                InvFilter.ALL -> true
                InvFilter.CRITICAL -> item.expiryStatus() in listOf(
                    ExpiryStatus.CRITICAL,
                    ExpiryStatus.EXPIRED
                )
                InvFilter.WARNING -> item.expiryStatus() in listOf(
                    ExpiryStatus.WARNING,
                    ExpiryStatus.SOON
                )
                InvFilter.VEGETABLE -> item.category == FoodCategory.VEGETABLE
                InvFilter.MEAT -> item.category == FoodCategory.MEAT
                InvFilter.DAIRY -> item.category == FoodCategory.DAIRY
                InvFilter.FRUIT -> item.category == FoodCategory.FRUIT
                InvFilter.SEAFOOD -> item.category == FoodCategory.SEAFOOD
            }
        }
        .filter {
            if (query.isBlank()) true else it.name.contains(query)
        }
        .sortedBy { it.daysLeft() }

    val criticalCount = inventory.count {
        it.expiryStatus() in listOf(
            ExpiryStatus.CRITICAL,
            ExpiryStatus.EXPIRED
        )
    }

    val warningCount = inventory.count {
        it.expiryStatus() in listOf(
            ExpiryStatus.WARNING,
            ExpiryStatus.SOON
        )
    }

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
                    onValueChange = { query = it },
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
                    InvFilter.ALL -> inventory.size
                    InvFilter.CRITICAL -> criticalCount
                    InvFilter.WARNING -> warningCount
                    else -> null
                }

                val chipAccent = when (f) {
                    InvFilter.CRITICAL -> colors.danger
                    InvFilter.WARNING -> colors.warning
                    else -> colors.accent
                }

                val chipBg = when {
                    active && f == InvFilter.CRITICAL -> colors.danger
                    active && f == InvFilter.WARNING -> colors.warning
                    active -> colors.accent
                    f == InvFilter.CRITICAL -> colors.dangerSoft
                    f == InvFilter.WARNING -> colors.warningSoft
                    else -> colors.surface
                }

                val chipBorder = when {
                    active -> chipAccent
                    f == InvFilter.CRITICAL ->
                        colors.danger.copy(alpha = 0.4f)
                    f == InvFilter.WARNING ->
                        colors.warning.copy(alpha = 0.4f)
                    else -> colors.border
                }

                val chipText = when {
                    active -> Color.White
                    f == InvFilter.CRITICAL -> colors.danger
                    f == InvFilter.WARNING -> Color(0xFF9B6B1F)
                    else -> colors.text
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
            items(filtered) { item ->
                InventoryRow(
                    colors,
                    item,
                    onClick = {
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
}

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
            Pair(colors.dangerSoft, colors.danger)

        ExpiryStatus.WARNING ->
            Pair(colors.warningSoft, Color(0xFF9B6B1F))

        ExpiryStatus.SOON ->
            Pair(colors.infoSoft, colors.info)

        ExpiryStatus.FRESH ->
            Pair(colors.accentSoft, colors.accentDeep)
    }

    val cat = item.category

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
            .clickable(onClick = onClick)
            .padding(12.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Color(cat.hexColor).copy(alpha = 0.1f)
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
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.dangerSoft)
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
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(tone.first)
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
