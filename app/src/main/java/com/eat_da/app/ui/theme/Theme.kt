package com.eatda.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class EatdaColors(
    val bg: Color,
    val bgSubtle: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val border: Color,
    val borderStrong: Color,
    val text: Color,
    val textMuted: Color,
    val textFaint: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentDeep: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val info: Color,
    val infoSoft: Color,
    val overlay: Color,
    val isA11y: Boolean = false,
    val isColorBlind: Boolean = false,
)

@Immutable
data class EatdaSizes(
    val fontBase: Int,
    val fontH: Int,
    val radius: Dp,
    val radiusSm: Dp,
    val touchTarget: Dp = 44.dp,
)

val DefaultEatdaColors = EatdaColors(
    bg = Cream,
    bgSubtle = CreamSubtle,
    surface = White,
    surfaceAlt = SurfaceAlt,
    border = BorderLight,
    borderStrong = BorderStrong,
    text = CharcoalDark,
    textMuted = CharcoalMuted,
    textFaint = CharcoalFaint,
    accent = GreenAccent,
    accentSoft = GreenSoft,
    accentDeep = GreenDeep,
    warning = AmberWarn,
    warningSoft = AmberSoft,
    danger = CoralDanger,
    dangerSoft = CoralSoft,
    info = BlueInfo,
    infoSoft = BlueSoft,
    overlay = Color(0x8C1F1B16),
)

val A11yEatdaColors = EatdaColors(
    bg = A11yBg,
    bgSubtle = A11yBgSubtle,
    surface = A11yBg,
    surfaceAlt = A11yBgSubtle,
    border = A11yBorder,
    borderStrong = A11yBorder,
    text = A11yText,
    textMuted = A11yTextMuted,
    textFaint = A11yTextFaint,
    accent = A11yGreen,
    accentSoft = A11yGreenSoft,
    accentDeep = A11yGreenDeep,
    warning = A11yAmber,
    warningSoft = A11yAmberSoft,
    danger = A11yDanger,
    dangerSoft = A11yDangerSoft,
    info = A11yInfo,
    infoSoft = A11yInfoSoft,
    overlay = Color(0xBF000000),
    isA11y = true,
)

val DefaultEatdaSizes = EatdaSizes(
    fontBase = 14,
    fontH = 22,
    radius = 16.dp,
    radiusSm = 10.dp,
)

val A11yEatdaSizes = EatdaSizes(
    fontBase = 18,
    fontH = 28,
    radius = 12.dp,
    radiusSm = 8.dp,
)

val ColorBlindEatdaColors = EatdaColors(
    bg = Cream,
    bgSubtle = CreamSubtle,
    surface = White,
    surfaceAlt = SurfaceAlt,
    border = BorderLight,
    borderStrong = BorderStrong,
    text = CharcoalDark,
    textMuted = CharcoalMuted,
    textFaint = CharcoalFaint,
    accent = CbAccent,
    accentSoft = CbAccentSoft,
    accentDeep = CbAccentDeep,
    warning = CbWarning,
    warningSoft = CbWarningSoft,
    danger = CbDanger,
    dangerSoft = CbDangerSoft,
    info = BlueInfo,
    infoSoft = BlueSoft,
    overlay = Color(0x8C1F1B16),
    isColorBlind = true,
)

val LocalEatdaColors = staticCompositionLocalOf { DefaultEatdaColors }
val LocalEatdaSizes = staticCompositionLocalOf { DefaultEatdaSizes }
