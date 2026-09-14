package com.eatda.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eatda.app.data.model.*
import com.eatda.app.ui.components.*
import com.eatda.app.ui.theme.*

@Composable
fun NotificationsScreen(
    colors: EatdaColors,
    sizes: EatdaSizes,
    notifications: List<AppNotification>,
) {
    val urgent = notifications.filter { it.urgent }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.bg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
    ) {
        item {
            Text("알림", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = colors.text,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 14.dp))
        }

        if (notifications.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Spacer(Modifier.height(8.dp))
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
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surface).padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(iconBg), contentAlignment = Alignment.Center) {
            EatdaIcon(icon, tint = iconFg, size = 18.dp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(n.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text, modifier = Modifier.weight(1f))
                Text(n.time, fontSize = 11.sp, color = colors.textFaint)
            }
            Text(n.body, fontSize = 12.sp, color = colors.textMuted, lineHeight = 18.sp, modifier = Modifier.padding(top = 3.dp))
        }
    }
}