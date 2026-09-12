package com.eatda.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eatda.app.ui.theme.*
import com.eatda.app.viewmodel.Screen

// ── Status Bar ───────────────────────────────────────────────────────────────
@Composable
fun EatdaStatusBar(colors: EatdaColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(colors.bg)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "9:30", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
        Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(Color(0xFF1A1A1A)))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("▲", fontSize = 10.sp, color = colors.text)
            Text("▐", fontSize = 10.sp, color = colors.text)
            Box(modifier = Modifier.width(22.dp).height(11.dp).border(1.dp, colors.text, RoundedCornerShape(2.dp))) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.78f)
                        .padding(2.dp)
                        .background(colors.text, RoundedCornerShape(1.dp))
                )
            }
        }
    }
}

// ── Top Bar ──────────────────────────────────────────────────────────────────
@Composable
fun EatdaTopBar(
    colors: EatdaColors,
    hasNotif: Boolean,
    onNotif: () -> Unit,
    onProfile: () -> Unit,
    profileActive: Boolean,
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showBack && onBack != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack)
                    .semantics { contentDescription = "뒤로 가기" },
                contentAlignment = Alignment.Center,
            ) {
                EatdaIcon(EatdaIcons.ChevronLeft, tint = colors.text, size = 18.dp)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(colors.accentSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    EatdaLogoMark(color = colors.accent, size = 20.dp)
                }
                Text("모아밥", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = colors.text)
            }
        }

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.dp, colors.border, CircleShape)
                .clickable(onClick = onNotif)
                .semantics { contentDescription = if (hasNotif) "알림, 새 알림 있음" else "알림" },
            contentAlignment = Alignment.Center,
        ) {
            EatdaIcon(EatdaIcons.Bell, tint = colors.text, size = 18.dp)
            if (hasNotif) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .align(Alignment.TopEnd)
                        .offset((-9).dp, 8.dp)
                        .clip(CircleShape)
                        .background(colors.danger)
                        .border(1.5.dp, colors.surface, CircleShape)
                )
            }
        }

        val profileBg = if (profileActive) colors.accentSoft else colors.surface
        val profileBorder = if (profileActive) colors.accent else colors.border
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(profileBg)
                .border(1.dp, profileBorder, CircleShape)
                .clickable(onClick = onProfile)
                .semantics { contentDescription = "내 프로필" },
            contentAlignment = Alignment.Center,
        ) {
            Text("길", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = colors.accent)
        }
    }
}

// ── Bottom Navigation ────────────────────────────────────────────────────────
@Composable
fun EatdaBottomNav(
    colors: EatdaColors,
    sizes: EatdaSizes,
    currentScreen: Screen,
    onNav: (Screen) -> Unit,
    onScan: () -> Unit,
) {
    val borderColor = colors.border
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(
                    color = borderColor,
                    start = Offset(0f, stroke / 2),
                    end = Offset(size.width, stroke / 2),
                    strokeWidth = stroke,
                )
            },
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            NavTab(colors, sizes, Screen.HOME,      EatdaIcons.Home,     "홈",    currentScreen, onNav)
            NavTab(colors, sizes, Screen.INVENTORY, EatdaIcons.Fridge,   "재고",  currentScreen, onNav)
            Spacer(Modifier.width(64.dp))
            NavTab(colors, sizes, Screen.RECIPES,   EatdaIcons.Leaf,     "레시피", currentScreen, onNav)
            NavTab(colors, sizes, Screen.SETTINGS,  EatdaIcons.Settings, "설정",  currentScreen, onNav)
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-22).dp)
                .clip(CircleShape)
                .background(colors.accent)
                .border(4.dp, colors.bg, CircleShape)
                .clickable(onClick = onScan)
                .semantics { contentDescription = "스캔하기"; role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            EatdaIcon(EatdaIcons.Camera, tint = Color.White, size = 24.dp)
        }
    }
}

@Composable
private fun RowScope.NavTab(
    colors: EatdaColors,
    sizes: EatdaSizes,
    screen: Screen,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    current: Screen,
    onNav: (Screen) -> Unit,
) {
    val active = current == screen
    val tint = if (active) colors.accent else colors.textMuted
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable { onNav(screen) }
            .padding(horizontal = 4.dp, vertical = 10.dp)
            .semantics { contentDescription = "$label${if (active) ", 현재 화면" else ""}"; role = Role.Tab },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        EatdaIcon(icon, tint = tint, size = if (sizes.isA11y) 24.dp else 22.dp)
        Text(
            label,
            fontSize = if (sizes.isA11y) 11.sp else 10.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = tint,
        )
    }
}

// ── Section Header ───────────────────────────────────────────────────────────
@Composable
fun SectionHeader(
    colors: EatdaColors,
    sizes: EatdaSizes,
    title: String,
    sub: String? = null,
    onMore: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(title, fontSize = if (sizes.isA11y) 17.sp else 15.sp, fontWeight = FontWeight.Bold, color = colors.text)
            if (sub != null) {
                Text(sub, fontSize = 11.sp, color = colors.textMuted, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (onMore != null) {
            Text(
                "전체보기 ›",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                modifier = Modifier
                    .clickable(onClick = onMore)
                    .semantics { contentDescription = "$title 전체보기" },
            )
        }
    }
}

// ── Logo mark ────────────────────────────────────────────────────────────────
@Composable
fun EatdaLogoMark(color: Color, size: Dp = 24.dp) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val r = this.size.width * 0.375f
        val cy = this.size.height / 2f
        drawCircle(color = color, radius = r, center = androidx.compose.ui.geometry.Offset(this.size.width * 0.375f, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
        drawCircle(color = color, radius = r, center = androidx.compose.ui.geometry.Offset(this.size.width * 0.625f, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
    }
}

// ── Toast ────────────────────────────────────────────────────────────────────
@Composable
fun EatdaToast(message: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF1F1B16))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(message, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Stat Card ────────────────────────────────────────────────────────────────
data class StatCardTone(val fg: Color, val bg: Color)

@Composable
fun StatCard(colors: EatdaColors, value: Int, label: String, tone: StatCardTone, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tone.bg)
            .then(
                if (colors.isA11y) Modifier.border(2.dp, colors.text, RoundedCornerShape(14.dp))
                else Modifier.border(1.dp, colors.border, RoundedCornerShape(14.dp))
            )
            .padding(horizontal = 16.dp, vertical = 18.dp)
            .semantics { contentDescription = "$label ${value}개" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value.toString(), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = tone.fg)
        Text(label, fontSize = 12.sp, color = colors.textMuted, fontWeight = FontWeight.Medium)
    }
}

private val EatdaSizes.isA11y: Boolean get() = fontBase == 18