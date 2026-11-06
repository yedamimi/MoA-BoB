package com.eatda.app.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eatda.app.R
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
            .background(if (colors.isColorBlind || colors.isA11y) colors.bg else Color.White)
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
                // 로고 이미지
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
                Text("모아밥", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = colors.text)
            }
        }

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (!colors.isColorBlind && !colors.isA11y) Color(0xFFF3F6F3) else colors.surface)
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
                        .border(1.5.dp, if (!colors.isColorBlind && !colors.isA11y) Color.White else colors.surface, CircleShape)
                )
            }
        }

        val figmaTop = !colors.isColorBlind && !colors.isA11y
        val profileBg = if (figmaTop) Color(0xFFE3EEE5) else if (profileActive) colors.accentSoft else colors.surface
        val profileBorder = if (figmaTop) Color.Transparent else if (profileActive) colors.accent else colors.border
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
    val figmaStyle = !colors.isColorBlind && !colors.isA11y
    val borderColor = if (figmaStyle) Color(0xFFE6EAE6) else colors.border
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (figmaStyle) Color.White else colors.surface)
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

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-9).dp)
                .width(64.dp)
                .clickable(onClick = onScan)
                .semantics { contentDescription = "촬영하기"; role = Role.Button },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(if (sizes.isA11y) 52.dp else 48.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .border(4.dp, if (figmaStyle) Color.White else colors.bg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                EatdaIcon(EatdaIcons.Camera, tint = Color.White, size = if (sizes.isA11y) 22.dp else 20.dp)
            }
            Text(
                "촬영",
                fontSize = if (sizes.isA11y) 11.sp else 10.sp,
                fontWeight = FontWeight.Bold,
                color = colors.accent,
            )
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
    val tint = if (active) colors.accent else if (!colors.isColorBlind && !colors.isA11y) Color(0xFF718077) else colors.textMuted
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable { onNav(screen) }
            .padding(horizontal = 4.dp, vertical = 10.dp)
            .semantics { contentDescription = "$label${if (active) ", 현재 화면" else ""}"; role = Role.Tab },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (screen == Screen.SETTINGS) {
            // 단순한 선형 톱니바퀴: 기존 설정 탭 동작과 선택 색상은 그대로 사용
            androidx.compose.foundation.Canvas(
                modifier = Modifier.size(if (sizes.isA11y) 24.dp else 20.dp)
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val outer = size.minDimension * 0.39f
                val inner = size.minDimension * 0.29f
                val stroke = 1.7.dp.toPx()
                for (i in 0 until 8) {
                    val angle = (i * Math.PI / 4.0).toFloat()
                    val dx = kotlin.math.cos(angle)
                    val dy = kotlin.math.sin(angle)
                    drawLine(
                        color = tint,
                        start = Offset(cx + inner * dx, cy + inner * dy),
                        end = Offset(cx + outer * dx, cy + outer * dy),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                }
                drawCircle(
                    color = tint,
                    radius = inner,
                    center = Offset(cx, cy),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
                )
                drawCircle(
                    color = tint,
                    radius = size.minDimension * 0.09f,
                    center = Offset(cx, cy),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
                )
            }
        } else {
            EatdaIcon(icon, tint = tint, size = if (sizes.isA11y) 24.dp else 20.dp)
        }
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