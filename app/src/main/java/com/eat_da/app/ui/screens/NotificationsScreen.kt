package com.eatda.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eatda.app.data.model.*
import com.eatda.app.ui.components.*
import com.eatda.app.ui.theme.*

// UI-only palette: preserve accessibility theme when color-blind mode is active.
private val EatdaColors.isColorBlindNotif: Boolean
    get() = this == ColorBlindEatdaColors

@Composable
fun NotificationsScreen(
    colors: EatdaColors,
    sizes: EatdaSizes,
    notifications: List<AppNotification>,
) {
    // Keep the original behavior: only urgent notifications are displayed.
    val urgent = notifications.filter { it.urgent }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
            .background(if (colors.isColorBlindNotif) colors.bg else Color.White),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
    ) {
        item {
            Text(
                "알림",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (colors.isColorBlindNotif) colors.text else Color(0xFF242C27),
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 4.dp),
            )
        }

        // 화면에는 긴급 알림만 보여주므로, 긴급 알림이 없을 때 안내를 표시
        // (긴급하지 않은 알림만 있을 때 빈 "긴급 알림" 제목만 남지 않도록)
        if (urgent.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        EatdaIcon(EatdaIcons.Bell, tint = colors.textFaint, size = 36.dp)
                        Text("새로운 알림이 없습니다", fontSize = 14.sp, color = colors.textFaint)
                        Text("유통기한 임박 식재료가 생기면 알림이 표시됩니다", fontSize = 12.sp, color = colors.textFaint)
                    }
                }
            }
        } else {
            item { SectionHeader(colors, sizes, "긴급 알림", "빠른 처리가 필요한 항목") }
            items(urgent) { n ->
                NotifCard(colors, n)
                Spacer(Modifier.height(10.dp))
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun NotifCard(colors: EatdaColors, n: AppNotification) {
    val (iconBg, iconFg, icon) = when (n.type) {
        NotifType.EXPIRY_SOON -> Triple(colors.dangerSoft, colors.danger, EatdaIcons.Clock)
        NotifType.ROT         -> Triple(colors.warningSoft, colors.warning, EatdaIcons.Leaf)
        NotifType.TIP         -> Triple(colors.accentSoft, colors.accentDeep, EatdaIcons.Info)
    }
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(
                1.dp,
                if (colors.isColorBlindNotif) colors.border else Color(0xFFE2E7E2),
                shape,
            )
            .padding(horizontal = 15.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            if (n.type == NotifType.EXPIRY_SOON) {
                Text("!", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = iconFg)
            } else {
                EatdaIcon(icon, tint = iconFg, size = 20.dp)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    n.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (colors.isColorBlindNotif) colors.text else Color(0xFF242C27),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(n.time, fontSize = 11.sp, color = colors.textFaint)
            }
            Text(
                n.body,
                fontSize = 13.sp,
                color = if (colors.isColorBlindNotif) colors.textMuted else Color(0xFF65736B),
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}
