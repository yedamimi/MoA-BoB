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
    a11yMode: Boolean = false,
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

    val pageBackground = colors.surface
    val canScan = allergens.isNotEmpty() && phase != AllergenScanPhase.ANALYZING
    val titleSize = if (a11yMode) 25.sp else 21.sp
    val bodySize = if (a11yMode) 17.sp else 13.sp
    val helperSize = if (a11yMode) 14.sp else 11.sp

    Column(
        modifier = Modifier.fillMaxSize().background(pageBackground),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(if (a11yMode) 52.dp else 42.dp).clip(RoundedCornerShape(13.dp))
                    .background(colors.surface).border(1.dp, colors.border, RoundedCornerShape(13.dp))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                EatdaIcon(EatdaIcons.Close, tint = colors.text, size = if (a11yMode) 21.dp else 16.dp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("알레르기 유발 성분 확인", fontSize = titleSize, lineHeight = if (a11yMode) 32.sp else 26.sp, fontWeight = FontWeight.ExtraBold, color = colors.text)
                Text(
                    if (allergens.isEmpty()) "등록된 알레르기가 없습니다"
                    else "내 알레르기: ${allergens.joinToString(", ") { it.name }}",
                    fontSize = if (a11yMode) 15.sp else 12.sp,
                    lineHeight = if (a11yMode) 21.sp else 16.sp,
                    color = colors.textMuted,
                )
            }
        }

        if (allergens.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🥛", fontSize = if (a11yMode) 58.sp else 46.sp)
                    Text("아직 등록한 알레르기가 없어요", fontSize = if (a11yMode) 22.sp else 18.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    Text("알레르기 성분을 확인하려면 먼저 등록해 주세요.", fontSize = bodySize, color = colors.textMuted, textAlign = TextAlign.Center)
                    Text(
                        "마이페이지에서 등록하기 ›",
                        fontSize = if (a11yMode) 18.sp else 14.sp,
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
                        Text("📦", fontSize = if (a11yMode) 66.sp else 54.sp)
                        Spacer(Modifier.height(18.dp))
                        Text("제품 뒷면의 원재료명·알레르기 표시 부분을 찍어주세요", fontSize = if (a11yMode) 20.sp else 16.sp, fontWeight = FontWeight.Bold, color = colors.text, textAlign = TextAlign.Center, lineHeight = if (a11yMode) 29.sp else 23.sp)
                        Text("글자가 화면에 꽉 차고 흔들리지 않게 찍으면 더 정확해요.", fontSize = bodySize, color = colors.textMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp), lineHeight = if (a11yMode) 25.sp else 20.sp)
                    }
                }
                AllergenScanPhase.ANALYZING -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = colors.accent, strokeWidth = if (a11yMode) 5.dp else 4.dp)
                            Text("성분표를 읽고 있어요...", fontSize = if (a11yMode) 20.sp else 15.sp, fontWeight = FontWeight.Bold, color = colors.text)
                            Text("내 알레르기 성분과 비교하는 중이에요", fontSize = if (a11yMode) 16.sp else 12.sp, color = colors.textMuted)
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
                                    a11yMode = a11yMode,
                                )
                            }
                        } else {
                            val parsed = result
                            val direct = parsed?.directHits.orEmpty()
                            val facility = parsed?.facilityHits.orEmpty()
                            when {
                                direct.isNotEmpty() || facility.isNotEmpty() -> {
                                    item { ChecklistResultBlock(colors, allergens.size, direct, facility, a11yMode) }
                                    item { SafetyNote(colors, "사진 인식 결과와 실제 표시가 다를 수 있어요. 섭취 전 제품 포장을 직접 확인해 주세요.", a11yMode) }
                                }
                                else -> {
                                    item {
                                        ResultCard(
                                            background = Color(0xFFE8F5E9),
                                            titleColor = Color(0xFF28743A),
                                            title = "등록한 알레르기 성분을 찾지 못했어요",
                                            body = "성분표 전체가 찍혔는지 확인해 주세요.",
                                            a11yMode = a11yMode,
                                        )
                                    }
                                }
                            }
                        }

                    }
                }
            }

            if (phase == AllergenScanPhase.IDLE) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SourceButton(colors, Modifier.weight(1f), "📷", "성분표 촬영", canScan, a11yMode) { takePicture.launch(photoUri) }
                    SourceButton(colors, Modifier.weight(1f), "🖼️", "앨범에서 선택", canScan, a11yMode) { pickImage.launch("image/*") }
                }
            } else if (phase == AllergenScanPhase.DONE) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ActionButton(colors, Modifier.weight(1f), "다시 스캔", false, a11yMode, onClick = ::resetScan)
                    ActionButton(colors, Modifier.weight(1f), "확인", true, a11yMode, onClick = onClose)
                }
            }
        }
    }
}

@Composable
private fun SourceButton(
    colors: EatdaColors,
    modifier: Modifier,
    icon: String,
    label: String,
    enabled: Boolean,
    a11yMode: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .heightIn(min = if (a11yMode) 66.dp else 52.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (enabled) colors.accentSoft else colors.surfaceAlt)
            .border(1.dp, colors.accent.copy(alpha = if (enabled) 0.25f else 0.1f), RoundedCornerShape(15.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = if (a11yMode) 18.dp else 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = if (a11yMode) 21.sp else 16.sp)
        Spacer(Modifier.width(if (a11yMode) 9.dp else 6.dp))
        Text(
            label,
            fontSize = if (a11yMode) 18.sp else 13.sp,
            lineHeight = if (a11yMode) 24.sp else 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) colors.accentDeep else colors.textMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ActionButton(
    colors: EatdaColors,
    modifier: Modifier,
    label: String,
    primary: Boolean,
    a11yMode: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .heightIn(min = if (a11yMode) 62.dp else 50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (primary) colors.accent else colors.surface)
            .border(if (primary) 0.dp else 1.dp, colors.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = if (a11yMode) 18.dp else 15.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = if (a11yMode) 18.sp else 14.sp,
            lineHeight = if (a11yMode) 24.sp else 19.sp,
            fontWeight = FontWeight.Bold,
            color = if (primary) Color.White else colors.text,
        )
    }
}

@Composable
private fun ResultCard(
    background: Color,
    titleColor: Color,
    title: String,
    body: String,
    a11yMode: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .padding(if (a11yMode) 22.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(if (a11yMode) 10.dp else 8.dp),
    ) {
        Text(title, fontSize = if (a11yMode) 21.sp else 17.sp, fontWeight = FontWeight.ExtraBold, color = titleColor)
        Text(body, fontSize = if (a11yMode) 17.sp else 13.sp, color = titleColor, lineHeight = if (a11yMode) 25.sp else 19.sp)
    }
}

@Composable
private fun DirectResultBlock(
    colors: EatdaColors,
    hits: List<com.eatda.app.data.model.AllergenIngredientHit>,
    a11yMode: Boolean,
) {
    val names = hits.distinctBy { it.allergenName }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .border(1.dp, colors.danger.copy(alpha = 0.28f), RoundedCornerShape(22.dp)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(colors.danger.copy(alpha = 0.09f))
                .padding(if (a11yMode) 22.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⚠", fontSize = if (a11yMode) 21.sp else 17.sp, color = colors.danger)
                Text("섭취 주의", fontSize = if (a11yMode) 17.sp else 13.sp, fontWeight = FontWeight.ExtraBold, color = colors.danger)
            }
            Text(
                "${names.joinToString(" · ") { it.allergenName }}이(가)\n들어 있어요",
                fontSize = if (a11yMode) 25.sp else 21.sp,
                lineHeight = if (a11yMode) 33.sp else 27.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.text,
            )
        }

        Column(
            modifier = Modifier.padding(if (a11yMode) 22.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(if (a11yMode) 15.dp else 11.dp),
        ) {
            names.forEachIndexed { index, hit ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            hit.allergenName,
                            fontSize = if (a11yMode) 19.sp else 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            hit.matchedTerms.distinct().forEach { term ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(colors.danger.copy(alpha = 0.10f))
                                        .padding(horizontal = if (a11yMode) 13.dp else 10.dp, vertical = if (a11yMode) 9.dp else 7.dp),
                                ) {
                                    Text(
                                        term,
                                        fontSize = if (a11yMode) 16.sp else 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.danger,
                                    )
                                }
                            }
                        }
                    }
                    if (index != names.lastIndex) {
                        androidx.compose.material3.HorizontalDivider(color = colors.border.copy(alpha = 0.55f))
                    }
                }
            }
            Text(
                "오른쪽은 성분표에서 찾은 표기예요",
                fontSize = if (a11yMode) 14.sp else 11.sp,
                lineHeight = if (a11yMode) 21.sp else 16.sp,
                color = colors.textMuted,
            )
        }
    }
}

@Composable
private fun FacilityResultBlock(
    colors: EatdaColors,
    hits: List<com.eatda.app.data.model.AllergenIngredientHit>,
    a11yMode: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .border(1.dp, colors.accent.copy(alpha = 0.28f), RoundedCornerShape(22.dp))
            .padding(if (a11yMode) 22.dp else 18.dp),
        verticalArrangement = Arrangement.spacedBy(if (a11yMode) 13.dp else 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ⓘ", fontSize = if (a11yMode) 21.sp else 17.sp, color = colors.accent)
            Text(
                "같은 시설에서 제조",
                fontSize = if (a11yMode) 18.sp else 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.accentDeep,
            )
        }
        Text(
            "원재료는 아니지만, 아래 성분을 사용하는 제품과 같은 시설에서 만들었다고 표시되어 있어요.",
            fontSize = if (a11yMode) 16.sp else 12.sp,
            color = colors.textMuted,
            lineHeight = if (a11yMode) 24.sp else 18.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            hits.map { it.allergenName }.distinct().forEach { name ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(colors.accentSoft)
                        .padding(horizontal = if (a11yMode) 14.dp else 11.dp, vertical = if (a11yMode) 10.dp else 7.dp),
                ) {
                    Text(
                        name,
                        fontSize = if (a11yMode) 16.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentDeep,
                    )
                }
            }
        }
        Text(
            "찾은 표기 · ${hits.flatMap { it.matchedTerms }.distinct().joinToString(", ")}",
            fontSize = if (a11yMode) 14.sp else 11.sp,
            lineHeight = if (a11yMode) 21.sp else 16.sp,
            color = colors.textMuted,
        )
    }
}


@Composable
private fun ChecklistResultBlock(
    colors: EatdaColors,
    registeredCount: Int,
    directHits: List<com.eatda.app.data.model.AllergenIngredientHit>,
    facilityHits: List<com.eatda.app.data.model.AllergenIngredientHit>,
    a11yMode: Boolean,
) {
    val direct = directHits.distinctBy { it.allergenName }
    val facility = facilityHits.distinctBy { it.allergenName }.filterNot { hit -> direct.any { it.allergenName == hit.allergenName } }
    val directCount = direct.size
    val facilityCount = facility.size
    val totalFound = directCount + facilityCount
    val danger = Color(0xFF9F3527)
    val dangerLight = Color(0xFFF9ECE9)
    val amber = Color(0xFF94600F)
    val amberLight = Color(0xFFFBF3E1)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (a11yMode) 19.dp else 14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(danger)
                .padding(if (a11yMode) 23.dp else 19.dp),
            verticalArrangement = Arrangement.spacedBy(if (a11yMode) 13.dp else 9.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⚠", fontSize = if (a11yMode) 22.sp else 17.sp, color = Color.White)
                Text(if (directCount > 0) "이 제품은 피하는 게 좋아요" else "같은 시설 제조 표시가 있어요", fontSize = if (a11yMode) 17.sp else 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Text(
                if (directCount > 0) "내 알레르기 ${registeredCount}개 중\n${directCount}개가 들어 있어요" else "내 알레르기 성분과\n같은 시설 제조 표시가 있어요",
                fontSize = if (a11yMode) 26.sp else 21.sp,
                lineHeight = if (a11yMode) 34.sp else 27.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ChecklistCountPill("포함 $directCount", Color.White, danger, a11yMode)
                if (facilityCount > 0) ChecklistCountPill("같은 시설 $facilityCount", Color(0xFFD9A69D), Color.White, a11yMode)
            }
        }

        Text("내 알레르기별 결과", fontSize = if (a11yMode) 17.sp else 13.sp, fontWeight = FontWeight.Bold, color = colors.textMuted)

        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            (direct.map { Triple(it, true, it.matchedTerms.distinct()) } + facility.map { Triple(it, false, it.matchedTerms.distinct()) })
                .forEachIndexed { index, (hit, isDirect, terms) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = if (a11yMode) 13.dp else 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(13.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(if (a11yMode) 48.dp else 39.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (isDirect) dangerLight else amberLight),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(if (isDirect) "×" else "!", fontSize = if (a11yMode) 28.sp else 22.sp, fontWeight = FontWeight.Medium, color = if (isDirect) danger else amber)
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(hit.allergenName, fontSize = if (a11yMode) 19.sp else 15.sp, fontWeight = FontWeight.Bold, color = colors.text)
                            Text(
                                (if (isDirect) "성분표 표기 · " else "같은 시설 표기 · ") + terms.joinToString(", "),
                                fontSize = if (a11yMode) 14.sp else 11.sp,
                                lineHeight = if (a11yMode) 20.sp else 16.sp,
                                color = colors.textMuted,
                            )
                        }
                        Text(
                            if (isDirect) "포함" else "같은 시설",
                            fontSize = if (a11yMode) 15.sp else 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDirect) danger else amber,
                        )
                    }
                    if (index != totalFound - 1) {
                        androidx.compose.material3.HorizontalDivider(color = colors.border.copy(alpha = 0.55f))
                    }
                }
        }
        Text(
            "‘같은 시설’은 원재료는 아니지만 해당 성분을 쓰는 제품과 같은 곳에서 만들었다는 뜻이에요.",
            fontSize = if (a11yMode) 14.sp else 11.sp,
            lineHeight = if (a11yMode) 21.sp else 16.sp,
            color = colors.textMuted,
        )
    }
}

@Composable
private fun ChecklistCountPill(label: String, background: Color, foreground: Color, a11yMode: Boolean) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(background)
            .padding(horizontal = if (a11yMode) 15.dp else 11.dp, vertical = if (a11yMode) 9.dp else 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = if (a11yMode) 15.sp else 11.sp, fontWeight = FontWeight.Bold, color = foreground)
    }
}

@Composable
private fun SafetyNote(colors: EatdaColors, message: String, a11yMode: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceAlt)
            .padding(if (a11yMode) 17.dp else 13.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("ⓘ", fontSize = if (a11yMode) 20.sp else 15.sp, color = colors.textMuted)
        Text(
            message,
            fontSize = if (a11yMode) 16.sp else 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.text,
            lineHeight = if (a11yMode) 24.sp else 18.sp,
        )
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