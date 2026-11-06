package com.eatda.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.eatda.app.data.model.Allergen
import com.eatda.app.data.model.AllergenIngredientParser
import com.eatda.app.data.model.AllergenIngredientResult
import com.eatda.app.ui.theme.EatdaColors
import com.eatda.app.ui.theme.ColorBlindEatdaColors
import com.eatda.app.util.HapticManager
import com.eatda.app.util.TtsService
import com.eatda.app.util.VisionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

private enum class AllergenScanPhase { IDLE, ANALYZING, DONE }

/** 제품 포장의 원재료명·알레르기 표시를 OCR로 읽어 등록 알레르기와 비교합니다. */
@Composable
fun AllergenScanView(
    colors: EatdaColors,
    allergens: List<Allergen>,
    ttsEnabled: Boolean,
    hapticEnabled: Boolean,
    onClose: () -> Unit,
    onToast: (String) -> Unit,
    onGoMyPage: () -> Unit,
    onScanComplete: (List<String>, List<String>) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var phase by remember { mutableStateOf(AllergenScanPhase.IDLE) }
    var result by remember { mutableStateOf<AllergenIngredientResult?>(null) }
    var readFailed by remember { mutableStateOf(false) }

    fun resetScan() {
        phase = AllergenScanPhase.IDLE
        result = null
        readFailed = false
    }

    fun analyze(uri: Uri) {
        phase = AllergenScanPhase.ANALYZING
        result = null
        readFailed = false
        scope.launch {
            val bytes = withContext(Dispatchers.IO) { loadAllergenImageJpeg(context, uri) }
            val text = bytes?.let { VisionService.extractText(it) }
            if (bytes == null || text.isNullOrBlank()) {
                readFailed = true
                phase = AllergenScanPhase.DONE
                onToast("성분표를 읽지 못했어요. 다시 촬영해 주세요.")
                if (ttsEnabled) TtsService.speak(context, "성분표를 읽지 못했습니다. 다시 촬영해 주세요.")
                return@launch
            }

            val parsed = AllergenIngredientParser.analyze(text, allergens)
            result = parsed
            phase = AllergenScanPhase.DONE
            onScanComplete(
                parsed.directHits.map { it.allergenName }.distinct(),
                parsed.facilityHits.map { it.allergenName }.distinct()
            )

            val directNames = parsed.directHits.map { it.allergenName }
            val facilityNames = parsed.facilityHits.map { it.allergenName }
            when {
                directNames.isNotEmpty() -> {
                    onToast("알레르기 성분이 확인됐어요: ${directNames.joinToString(", ")}")
                    if (hapticEnabled) HapticManager.allergenAlert(context)
                    if (ttsEnabled) {
                        val names = directNames.joinToString("와 ")
                        val facilityMessage = if (facilityNames.isNotEmpty()) {
                            " ${facilityNames.joinToString(" 및 ")} 성분을 사용하는 제품과 같은 시설에서 제조되었습니다."
                        } else ""
                        TtsService.speak(context, "주의하세요. $names 성분이 들어 있습니다.$facilityMessage")
                    }
                }
                facilityNames.isNotEmpty() -> {
                    onToast("같은 시설 제조 표시가 확인됐어요: ${facilityNames.joinToString(", ")}")
                    if (ttsEnabled) {
                        TtsService.speak(
                            context,
                            "${facilityNames.joinToString(" 및 ")} 성분을 사용하는 제품과 같은 시설에서 제조되었습니다."
                        )
                    }
                }
                else -> {
                    onToast("등록한 알레르기 성분을 찾지 못했어요.")
                    if (ttsEnabled) TtsService.speak(context, "등록한 알레르기 성분을 찾지 못했습니다.")
                }
            }
        }
    }

    val photoUri = remember(context) {
        val dir = File(context.cacheDir, "allergen-labels").apply { mkdirs() }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(dir, "allergen-label.jpg"),
        )
    }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) analyze(photoUri)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) analyze(uri)
    }

    BackHandler(onBack = onClose)

    val pageBackground = if (colors == ColorBlindEatdaColors) colors.bg else Color.White
    val canScan = allergens.isNotEmpty() && phase != AllergenScanPhase.ANALYZING

    Column(
        modifier = Modifier.fillMaxSize().background(pageBackground),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                    .background(colors.surface).border(1.dp, colors.border, RoundedCornerShape(13.dp))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                EatdaIcon(EatdaIcons.Close, tint = colors.text, size = 16.dp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("알레르기 유발 성분 확인", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = colors.text)
                Text(
                    if (allergens.isEmpty()) "등록된 알레르기가 없습니다"
                    else "내 알레르기: ${allergens.joinToString(", ") { it.name }}",
                    fontSize = 12.sp,
                    color = colors.textMuted,
                )
            }
        }

        if (allergens.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🥛", fontSize = 46.sp)
                    Text("아직 등록한 알레르기가 없어요", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    Text("알레르기 성분을 확인하려면 먼저 등록해 주세요.", fontSize = 13.sp, color = colors.textMuted, textAlign = TextAlign.Center)
                    Text(
                        "마이페이지에서 등록하기 ›",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onGoMyPage).padding(10.dp),
                    )
                }
            }
        } else {
            when (phase) {
                AllergenScanPhase.IDLE -> {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("📦", fontSize = 54.sp)
                        Spacer(Modifier.height(18.dp))
                        Text("제품 뒷면의 원재료명·알레르기 표시 부분을 찍어주세요", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text, textAlign = TextAlign.Center, lineHeight = 23.sp)
                        Text("글자가 화면에 꽉 차고 흔들리지 않게 찍으면 더 정확해요.", fontSize = 13.sp, color = colors.textMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp), lineHeight = 20.sp)
                    }
                }
                AllergenScanPhase.ANALYZING -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = colors.accent)
                            Text("성분표를 읽고 있어요...", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.text)
                            Text("내 알레르기 성분과 비교하는 중이에요", fontSize = 12.sp, color = colors.textMuted)
                        }
                    }
                }
                AllergenScanPhase.DONE -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (readFailed) {
                            item {
                                ResultCard(
                                    background = Color(0xFFFFF4E5),
                                    titleColor = Color(0xFF9B6B1F),
                                    title = "성분표를 잘 읽지 못했어요",
                                    body = "밝은 곳에서 글자가 잘 보이게 다시 찍어주세요.",
                                )
                            }
                        } else {
                            val parsed = result
                            val direct = parsed?.directHits.orEmpty()
                            val facility = parsed?.facilityHits.orEmpty()
                            when {
                                direct.isNotEmpty() -> {
                                    item { DirectResultBlock(direct) }
                                    item { EvidenceBlock(colors, "성분표에서 확인된 표기", direct) }
                                    if (facility.isNotEmpty()) {
                                        item { FacilityResultBlock(colors, facility) }
                                    }
                                    item { SafetyNote(colors, "섭취 전 제품 포장의 성분표를 다시 확인해 주세요.") }
                                }
                                facility.isNotEmpty() -> {
                                    item { FacilityResultBlock(colors, facility) }
                                    item { SafetyNote(colors, "민감하다면 섭취 전 제품 포장을 꼼꼼히 확인해 주세요.") }
                                }
                                else -> {
                                    item {
                                        ResultCard(
                                            background = Color(0xFFE8F5E9),
                                            titleColor = Color(0xFF28743A),
                                            title = "등록한 알레르기 성분을 찾지 못했어요",
                                            body = "성분표 전체가 찍혔는지 확인해 주세요.",
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Text(
                                "사진 인식 결과라 실제와 다를 수 있어요. 섭취 전 제품 포장을 직접 확인하세요.",
                                fontSize = 10.sp,
                                color = colors.textFaint,
                                lineHeight = 15.sp,
                            )
                        }
                    }
                }
            }

            if (phase == AllergenScanPhase.IDLE) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SourceButton(colors, Modifier.weight(1f), "📷", "성분표 촬영", canScan) { takePicture.launch(photoUri) }
                    SourceButton(colors, Modifier.weight(1f), "🖼️", "앨범에서 선택", canScan) { pickImage.launch("image/*") }
                }
            } else if (phase == AllergenScanPhase.DONE) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ActionButton(colors, Modifier.weight(1f), "다시 스캔", false, onClick = ::resetScan)
                    ActionButton(colors, Modifier.weight(1f), "확인", true, onClick = onClose)
                }
            }
        }
    }
}

@Composable
private fun SourceButton(colors: EatdaColors, modifier: Modifier, icon: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(15.dp))
            .background(if (enabled) colors.accentSoft else colors.surfaceAlt)
            .border(1.dp, colors.accent.copy(alpha = if (enabled) 0.25f else 0.1f), RoundedCornerShape(15.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (enabled) colors.accentDeep else colors.textMuted)
    }
}

@Composable
private fun ActionButton(colors: EatdaColors, modifier: Modifier, label: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(14.dp))
            .background(if (primary) colors.accent else colors.surface)
            .border(if (primary) 0.dp else 1.dp, colors.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (primary) Color.White else colors.text)
    }
}

@Composable
private fun ResultCard(background: Color, titleColor: Color, title: String, body: String) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = titleColor)
        Text(body, fontSize = 13.sp, color = titleColor, lineHeight = 19.sp)
    }
}

@Composable
private fun DirectResultBlock(hits: List<com.eatda.app.data.model.AllergenIngredientHit>) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFCEBE9)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("⚠", fontSize = 19.sp)
            Text("알레르기 성분이 포함되어 있어요", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB3261E))
        }
        Text(hits.joinToString(" · ") { it.allergenName }, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFA52A22))
        Text("성분표에서 직접 확인된 알레르기 성분입니다.", fontSize = 12.sp, color = Color(0xFF8C3932))
    }
}

@Composable
private fun EvidenceBlock(colors: EatdaColors, heading: String, hits: List<com.eatda.app.data.model.AllergenIngredientHit>) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(heading, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text)
        hits.forEach { hit ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(hit.allergenName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    hit.matchedTerms.distinct().forEach { term ->
                        Box(modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Color(0xFFFCEBE9)).padding(horizontal = 11.dp, vertical = 7.dp)) {
                            Text(term, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFB3261E))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FacilityResultBlock(colors: EatdaColors, hits: List<com.eatda.app.data.model.AllergenIngredientHit>) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFF4E3)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ⓘ", fontSize = 18.sp, color = Color(0xFF9B6B1F))
            Text("같은 시설에서 제조되는 성분", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF8A5B18))
        }
        Text(
            "직접 원재료로 확인된 것은 아니지만, 아래 성분을 사용하는 제품과 같은 제조 시설에서 생산되었다는 표시가 있습니다.",
            fontSize = 12.sp, color = Color(0xFF80551E), lineHeight = 18.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            hits.map { it.allergenName }.distinct().forEach { name ->
                Box(modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Color(0xFFFFE8C2)).padding(horizontal = 11.dp, vertical = 7.dp)) {
                    Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8A5B18))
                }
            }
        }
        Text(
            "표시에서 찾은 표기: ${hits.flatMap { it.matchedTerms }.distinct().joinToString(", ") { "\"$it\"" }}",
            fontSize = 11.sp, color = Color(0xFF8A5B18),
        )
    }
}

@Composable
private fun SafetyNote(colors: EatdaColors, message: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ⓘ", fontSize = 15.sp, color = colors.danger)
        Text(message, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.danger, lineHeight = 18.sp)
    }
}

private fun loadAllergenImageJpeg(context: Context, uri: Uri, maxSide: Int = 2400): ByteArray? = runCatching {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2

    val bitmap = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return@runCatching null

    ByteArrayOutputStream().use { output ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        bitmap.recycle()
        output.toByteArray()
    }
}.getOrNull()
