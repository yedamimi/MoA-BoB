package com.eatda.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eatda.app.ui.components.*
import com.eatda.app.ui.theme.*
import com.eatda.app.util.TtsService
import com.eatda.app.viewmodel.AppSettings
import com.eatda.app.viewmodel.FontScale
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    colors: EatdaColors,
    sizes: EatdaSizes,
    settings: AppSettings,
    onUpdateSettings: (AppSettings) -> Unit,
    onGoMyPage: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.bg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
    ) {
        item {
            Text("설정", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = colors.text, modifier = Modifier.padding(horizontal = 4.dp, vertical = 14.dp))
        }

        item {
            SettingsGroup(colors, "알림") {
                SettingsRow(
                    colors, sizes, EatdaIcons.Warn, "알레르기 유발 제품 알림", "스캔 시 알레르기 식재료 감지",
                    right = {
                        EatdaToggle(colors, on = settings.notifAllergen, label = "알레르기 알림") {
                            onUpdateSettings(settings.copy(notifAllergen = it))
                        }
                    },
                )
                SettingsDivider(colors)
                SettingsRow(
                    colors, sizes, EatdaIcons.Clock, "유통기한 D-DAY 알림", null,
                    right = {
                        EatdaToggle(colors, on = settings.notifExpiryDDay, label = "D-DAY 알림") {
                            onUpdateSettings(settings.copy(notifExpiryDDay = it))
                        }
                    },
                )
                SettingsDivider(colors)
                SettingsRow(
                    colors, sizes, EatdaIcons.Clock, "유통기한 D-2 알림", null,
                    right = {
                        EatdaToggle(colors, on = settings.notifExpiryD2, label = "D-2 알림") {
                            onUpdateSettings(settings.copy(notifExpiryD2 = it))
                        }
                    },
                )
                SettingsDivider(colors)
                SettingsRow(
                    colors, sizes, EatdaIcons.Clock, "유통기한 D-5 알림", null,
                    right = {
                        EatdaToggle(colors, on = settings.notifExpiryD5, label = "D-5 알림") {
                            onUpdateSettings(settings.copy(notifExpiryD5 = it))
                        }
                    },
                )
                SettingsDivider(colors)
                SettingsRow(
                    colors, sizes, EatdaIcons.Clock, "유통기한 D-7 알림", null,
                    right = {
                        EatdaToggle(colors, on = settings.notifExpiryD7, label = "D-7 알림") {
                            onUpdateSettings(settings.copy(notifExpiryD7 = it))
                        }
                    },
                )
                SettingsDivider(colors)
                SettingsRow(
                    colors, sizes, EatdaIcons.Sparkle, "레시피 추천", null,
                    last = true,
                    right = {
                        EatdaToggle(colors, on = settings.notifRecipe, label = "레시피 추천 알림") {
                            onUpdateSettings(settings.copy(notifRecipe = it))
                        }
                    },
                )
            }
            Spacer(Modifier.height(18.dp))
        }

        item {
            SettingsGroup(colors, "접근성") {
                SettingsRow(
                    colors, sizes, EatdaIcons.Home, "간소화 홈 화면", "핵심 알림만 크게 표시",
                    right = {
                        EatdaToggle(colors, on = settings.simplifiedHome, label = "간소화 홈") {
                            onUpdateSettings(settings.copy(simplifiedHome = it))
                        }
                    },
                )
                SettingsDivider(colors)
                SettingsRow(
                    colors, sizes, EatdaIcons.A11y, "접근성 모드", "고대비 + 큰 글씨로 전환",
                    accent = true,
                    right = {
                        EatdaToggle(colors, on = settings.a11yMode, label = "접근성 모드") {
                            onUpdateSettings(settings.copy(a11yMode = it))
                        }
                    },
                )
                SettingsDivider(colors)
                SettingsRow(
                    colors, sizes, EatdaIcons.Volume, "음성 안내 (TTS)", "알림과 경고를 음성으로 재생",
                    right = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (settings.ttsEnabled) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.accentSoft)
                                        .clickable { scope.launch { TtsService.speak(context, "안녕하세요. 모아밥 음성 안내입니다.") } }
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                ) {
                                    Text("테스트 ▶", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
                                }
                            }
                            EatdaToggle(colors, on = settings.ttsEnabled, label = "음성 안내") {
                                onUpdateSettings(settings.copy(ttsEnabled = it))
                            }
                        }
                    },
                )
                SettingsDivider(colors)
                SettingsRow(
                    colors, sizes, EatdaIcons.FontSize, "글꼴 크기", null,
                    right = {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            FontScale.entries.forEach { scale ->
                                val active = settings.fontScale == scale
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) colors.accent else colors.surfaceAlt)
                                        .border(1.dp, if (active) colors.accent else colors.border, RoundedCornerShape(8.dp))
                                        .clickable { onUpdateSettings(settings.copy(fontScale = scale)) }
                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                        .semantics { contentDescription = "글꼴 ${scale.label}" },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        scale.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.White else colors.text,
                                    )
                                }
                            }
                        }
                    },
                )
                SettingsDivider(colors)
                SettingsRow(
                    colors, sizes, EatdaIcons.Eye, "색상 접근성 모드", "파랑-노랑 계열로 색상 재배치",
                    last = true,
                    right = {
                        EatdaToggle(colors, on = settings.colorBlindMode, label = "색상 접근성 모드") {
                            onUpdateSettings(settings.copy(colorBlindMode = it))
                        }
                    },
                )
            }
            Spacer(Modifier.height(18.dp))
        }

        item {
            SettingsGroup(colors, "냉장고") {
                SettingsRow(
                    colors, sizes, EatdaIcons.Camera, "USB 웹캠 연결", "갤럭시 + 외장 카메라 페어링",
                    last = true,
                    right = {
                        Text("연결됨", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
                    },
                )
            }
            Spacer(Modifier.height(18.dp))
        }

        item {
            SettingsGroup(colors, "계정") {
                SettingsRow(
                    colors, sizes, EatdaIcons.User, "프로필", "사용자 · user@example.com",
                    onClick = onGoMyPage,
                    right = { EatdaIcon(EatdaIcons.ChevronRight, tint = colors.textFaint, size = 18.dp) },
                )
                SettingsDivider(colors)
                SettingsRow(
                    colors, sizes, EatdaIcons.Info, "앱 정보", "모아밥 v1.0.2 · MVP",
                    last = true,
                    right = { EatdaIcon(EatdaIcons.ChevronRight, tint = colors.textFaint, size = 18.dp) },
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingsGroup(colors: EatdaColors, title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.06.sp,
        color = colors.textMuted,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp)),
        content = content,
    )
}

@Composable
private fun SettingsRow(
    colors: EatdaColors,
    sizes: EatdaSizes,
    icon: ImageVector,
    label: String,
    sub: String?,
    accent: Boolean = false,
    last: Boolean = false,
    onClick: (() -> Unit)? = null,
    right: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (accent) colors.accentSoft else colors.surfaceAlt),
            contentAlignment = Alignment.Center,
        ) {
            EatdaIcon(icon, tint = if (accent) colors.accentDeep else colors.textMuted, size = 18.dp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                fontSize = if (sizes.isA11y) 16.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.text,
            )
            if (sub != null) {
                Text(sub, fontSize = 12.sp, color = colors.textMuted, modifier = Modifier.padding(top = 1.dp))
            }
        }
        right()
    }
}

@Composable
private fun SettingsDivider(colors: EatdaColors) {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(start = 60.dp).background(colors.border))
}

@Composable
private fun EatdaToggle(colors: EatdaColors, on: Boolean, label: String, onChange: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (on) colors.accent else colors.borderStrong)
            .clickable { onChange(!on) }
            .padding(2.dp)
            .semantics {
                role = Role.Switch
                stateDescription = if (on) "켜짐" else "꺼짐"
                contentDescription = "$label ${if (on) "켜짐" else "꺼짐"}"
            },
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

private val EatdaSizes.isA11y: Boolean get() = fontBase == 18