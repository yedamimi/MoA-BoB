package com.eatda.app.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eatda.app.data.foodEmoji
import com.eatda.app.data.model.ScanMode
import com.eatda.app.data.model.ScanResult
import com.eatda.app.ui.theme.*
import com.eatda.app.util.CameraLabelerService
import kotlinx.coroutines.launch

@Composable
fun CameraScanView(
    mode: ScanMode,
    colors: EatdaColors,
    results: List<ScanResult>,
    onAddResults: (List<ScanResult>) -> Unit,
    onClose: () -> Unit,
    onConfirm: (List<ScanResult>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    var feedbackMsg by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            isScanning = true
            feedbackMsg = "분석 중..."
            scope.launch {
                val detected = CameraLabelerService.detectFood(bitmap)
                if (detected.isNotEmpty()) {
                    onAddResults(detected)
                    feedbackMsg = "${detected.first().name} 등 ${detected.size}개 감지됨"
                } else {
                    feedbackMsg = "식재료를 인식하지 못했어요. 다시 시도해 주세요."
                }
                isScanning = false
            }
        }
    }

    val modeLabel = when (mode) {
        ScanMode.IN       -> "입고 스캔"
        ScanMode.OUT      -> "출고 스캔"
        ScanMode.FRESH    -> "신선도 스캔"
        ScanMode.ALLERGEN -> "알레르기 스캔"
    }
    val modeColor = when (mode) {
        ScanMode.IN       -> colors.accent
        ScanMode.OUT      -> Color(0xFFD17231)
        ScanMode.FRESH    -> Color(0xFF5A4FB5)
        ScanMode.ALLERGEN -> colors.danger
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    EatdaIcon(EatdaIcons.Close, tint = colors.text, size = 16.dp)
                }
                Text(modeLabel, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colors.text)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(190.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .border(1.5.dp, if (isScanning) modeColor.copy(alpha = 0.4f) else colors.border, RoundedCornerShape(20.dp))
                    .clickable(enabled = !isScanning) { cameraLauncher.launch(null) },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isScanning) colors.surfaceAlt else modeColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        EatdaIcon(
                            EatdaIcons.Camera,
                            tint = if (isScanning) colors.textMuted else modeColor,
                            size = 32.dp,
                        )
                    }
                    Text(
                        if (isScanning) feedbackMsg else "탭하여 식재료 촬영",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isScanning) colors.textMuted else colors.text,
                    )
                    if (!isScanning) {
                        Text("카메라로 식재료를 찍으면 자동으로 인식합니다", fontSize = 11.sp, color = colors.textFaint)
                    }
                }
            }

            if (feedbackMsg.isNotEmpty() && !isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (feedbackMsg.contains("못")) colors.warningSoft else colors.accentSoft
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        feedbackMsg,
                        fontSize = 12.sp,
                        color = if (feedbackMsg.contains("못")) Color(0xFF9B6B1F) else colors.accentDeep,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            if (results.isNotEmpty()) {
                Text(
                    "인식된 항목 ${results.size}개",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { it.id }) { result ->
                        CameraResultRow(colors, result, modeColor)
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        EatdaIcon(EatdaIcons.Package, tint = colors.textFaint, size = 32.dp)
                        Text("아직 인식된 항목이 없습니다", fontSize = 13.sp, color = colors.textFaint)
                        Text("위 버튼으로 식재료를 촬영하세요", fontSize = 11.sp, color = colors.textFaint)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        .clickable(onClick = onClose)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("취소", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.text)
                }
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (results.isEmpty()) colors.surfaceAlt else modeColor)
                        .clickable(enabled = results.isNotEmpty()) { onConfirm(results) }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (results.isEmpty()) "항목을 먼저 촬영하세요" else "확인 (${results.size}개)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (results.isEmpty()) colors.textMuted else Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraResultRow(colors: EatdaColors, result: ScanResult, accentColor: Color) {
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
                .background(accentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(foodEmoji[result.name] ?: result.name.first().toString(), fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(result.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
            result.confidence?.let { conf ->
                Text("신뢰도 ${(conf * 100).toInt()}%", fontSize = 11.sp, color = colors.textMuted)
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(accentColor.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text("인식됨", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}
