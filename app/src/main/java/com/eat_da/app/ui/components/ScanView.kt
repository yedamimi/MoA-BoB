package com.eatda.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eatda.app.data.foodEmoji
import com.eatda.app.data.model.*
import com.eatda.app.ui.theme.*
import com.eatda.app.util.HapticManager
import com.eatda.app.util.VisionService
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ScanView(
    mode: ScanMode,
    colors: EatdaColors,
    allergens: List<Allergen>,
    scanResults: List<ScanResult>,   // Firebase에서 수신한 YOLO 감지 결과
    scanPhase: String,               // "idle" | "scanning" | "detected"
    onClose: () -> Unit,
    onConfirm: (List<ScanResult>) -> Unit,
) {
    val context    = LocalContext.current
    val isIn       = mode == ScanMode.IN
    val isFresh    = mode == ScanMode.FRESH
    val isAllergen = mode == ScanMode.ALLERGEN

    val accent = when {
        isIn || isAllergen -> colors.accent
        isFresh            -> Color(0xFF5A4FB5)
        else               -> Color(0xFFD17231)
    }
    val accentSoft = when {
        isIn || isAllergen -> colors.accentSoft
        isFresh            -> Color(0xFFECEAF8)
        else               -> Color(0xFFFBE9D9)
    }
    val accentDeep = when {
        isIn || isAllergen -> colors.accentDeep
        isFresh            -> Color(0xFF3B3287)
        else               -> Color(0xFF7E3F1A)
    }

    val titleText = when (mode) {
        ScanMode.IN       -> "식재료 입고"
        ScanMode.OUT      -> "식재료 출고"
        ScanMode.FRESH    -> "신선도 확인"
        ScanMode.ALLERGEN -> "알레르기 식재료 스캔"
    }
    val subText = when (mode) {
        ScanMode.IN, ScanMode.ALLERGEN -> "YOLO AI · 웹캠 입고 모드"
        ScanMode.OUT                   -> "YOLO AI · 웹캠 출고 모드"
        ScanMode.FRESH                 -> "OpenCV · 웹캠 부패도 분석"
    }

    // ── 알레르기 감지 → HapticManager 진동 ───────────────────────────────────
    var allergyAlert by remember { mutableStateOf<ScanResult?>(null) }
    LaunchedEffect(scanResults) {
        val found = scanResults.firstOrNull { r ->
            r.isAllergen && allergens.any { a -> a.name == r.name }
        }
        if (found != null && allergyAlert == null) {
            allergyAlert = found
            HapticManager.allergenAlert(context)
        }
    }

    // ── 유통기한 OCR ─────────────────────────────────────────────────────────
    val scope        = rememberCoroutineScope()
    var ocrTargetId  by remember { mutableStateOf<String?>(null) }
    var ocrLoadingId by remember { mutableStateOf<String?>(null) }
    var ocrResults   by remember { mutableStateOf<Map<String, LocalDate>>(emptyMap()) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        val id = ocrTargetId ?: return@rememberLauncherForActivityResult
        ocrTargetId = null
        if (bitmap == null) { ocrLoadingId = null; return@rememberLauncherForActivityResult }
        scope.launch {
            ocrLoadingId = id
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
            val date = VisionService.extractExpiryDate(stream.toByteArray())
            if (date != null) ocrResults = ocrResults + (id to date)
            ocrLoadingId = null
        }
    }

    val pulseDot by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseDot",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        // ── 헤더 ─────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                EatdaIcon(EatdaIcons.ChevronLeft, tint = colors.text, size = 20.dp)
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(titleText, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colors.text)
                Text(subText, fontSize = 11.sp, color = colors.textMuted, modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(Modifier.size(38.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // ── 웹캠 뷰 영역 ─────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(accent)
                                .graphicsLayer { alpha = pulseDot }
                        )
                        Text("웹캠 인식 중", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.text)
                        Text("YOLO AI 분석 중...", fontSize = 12.sp, color = colors.textMuted)
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // ── 스캔 중 상태 표시 ────────────────────────────────────────────
            if (scanPhase == "scanning") {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentSoft)
                            .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accent)
                                .graphicsLayer { alpha = pulseDot },
                        )
                        Text(
                            when {
                                isIn || isAllergen -> "YOLO v8 · 웹캠 입고 인식 중"
                                isFresh            -> "OpenCV · 웹캠 부패 면적 측정 중"
                                else               -> "YOLO v8 · 웹캠 출고 인식 중"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentDeep,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }

            // ── 결과 목록 헤더 ───────────────────────────────────────────────
            item {
                Text(
                    "인식 결과",
                    fontSize = 12.sp,
                    color = colors.textMuted,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
            }

            // ── 결과 없을 때 플레이스홀더 ────────────────────────────────────
            if (scanResults.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (scanPhase == "scanning") "인식 대기 중..." else "웹캠 스캔을 시작하면 결과가 여기에 표시됩니다",
                            fontSize = 12.sp,
                            color = colors.textFaint,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Firebase 수신 결과 행 ────────────────────────────────────────
            items(scanResults) { result ->
                ScanResultRow(colors, result, accent, isFresh, allergens)
                if (isIn) {
                    Spacer(Modifier.height(3.dp))
                    ExpiryOcrRow(
                        colors = colors,
                        extractedDate = ocrResults[result.id],
                        isLoading = ocrLoadingId == result.id,
                        onPhotoTap = {
                            ocrTargetId = result.id
                            cameraLauncher.launch(null)
                        },
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── 확정 버튼 ────────────────────────────────────────────────────
            if (scanPhase == "detected" && scanResults.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isFresh) accent else Color.Transparent)
                                .border(
                                    if (isFresh) 0.dp else 1.5.dp,
                                    accent,
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable { onConfirm(scanResults) }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                EatdaIcon(
                                    if (isFresh) EatdaIcons.Leaf else EatdaIcons.Package,
                                    tint = if (isFresh) Color.White else accent,
                                    size = 16.dp,
                                )
                                Text(
                                    if (isFresh) "결과 확인 완료"
                                    else "${scanResults.size}${if (isIn || isAllergen) "개 재고에 입고" else "개 재고에서 출고"}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFresh) Color.White else accent,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                                .clickable { onConfirm(emptyList()) }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                EatdaIcon(EatdaIcons.Refresh, tint = colors.text, size = 16.dp)
                                Text("다시 스캔", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // ── 알레르기 경고 다이얼로그 ─────────────────────────────────────────────
    allergyAlert?.let { item ->
        AllergyAlert(colors = colors, item = item, onDismiss = { allergyAlert = null })
    }
}

@Composable
private fun ExpiryOcrRow(
    colors: EatdaColors,
    extractedDate: LocalDate?,
    isLoading: Boolean,
    onPhotoTap: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceAlt)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            EatdaIcon(EatdaIcons.Clock, tint = colors.textMuted, size = 13.dp)
            Text(
                when {
                    isLoading             -> "OCR 분석 중..."
                    extractedDate != null -> extractedDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                    else                  -> "유통기한 미등록"
                },
                fontSize = 12.sp,
                fontWeight = if (extractedDate != null && !isLoading) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isLoading             -> colors.textMuted
                    extractedDate != null -> colors.text
                    else                  -> colors.textFaint
                },
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (!isLoading) colors.accentSoft else colors.surfaceAlt)
                .then(if (!isLoading) Modifier.clickable(onClick = onPhotoTap) else Modifier)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                EatdaIcon(
                    EatdaIcons.Camera,
                    tint = if (!isLoading) colors.accent else colors.textFaint,
                    size = 12.dp,
                )
                Text(
                    when {
                        isLoading             -> "분석 중"
                        extractedDate != null -> "다시 촬영"
                        else                  -> "유통기한 촬영"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (!isLoading) colors.accent else colors.textFaint,
                )
            }
        }
    }
}

@Composable
private fun ScanResultRow(
    colors: EatdaColors,
    result: ScanResult,
    accent: Color,
    isFresh: Boolean,
    allergens: List<Allergen>,
) {
    val isAllergic = result.isAllergen && allergens.any { it.name == result.name }
    val cat        = result.category
    val label      = if (result.qty.isNotBlank()) "${result.name} ${result.qty}" else result.name

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (cat != null) Color(cat.hexColor).copy(alpha = 0.1f)
                    else accent.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(foodEmoji[result.name] ?: result.name.first().toString(), fontSize = 22.sp)
        }

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
            if (isAllergic) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.dangerSoft)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text("알레르기", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = colors.danger)
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            if (isFresh) {
                val fColor = when (result.freshnessStatus) {
                    FreshnessStatus.FRESH     -> accent
                    FreshnessStatus.ATTENTION -> Color(0xFF9B6B1F)
                    FreshnessStatus.ROTTEN    -> colors.danger
                    null                      -> accent
                }
                Text("${result.freshness}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fColor)
                Text("부패 ${result.rotPct}%", fontSize = 10.sp, color = colors.textFaint)
            } else if (result.confidence != null) {
                Text(
                    "${"%.1f".format(result.confidence * 100)}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
            } else {
                Text("감지됨", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent)
            }
        }
    }
}
