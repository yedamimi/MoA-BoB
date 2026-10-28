package com.eatda.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.eatda.app.data.ReceiptEntry
import com.eatda.app.data.foodEmoji
import com.eatda.app.data.parseReceipt
import com.eatda.app.ui.theme.*
import com.eatda.app.util.VisionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class ReceiptPhase { IDLE, ANALYZING, DONE }

// 영수증 스캔 → 식재료 인식 → 확인·수정 → 입고
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScanView(
    colors: EatdaColors,
    onClose: () -> Unit,
    onConfirm: (List<ReceiptEntry>) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var phase by remember { mutableStateOf(ReceiptPhase.IDLE) }
    var entries by remember { mutableStateOf<List<ReceiptEntry>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var editingDateId by remember { mutableStateOf<Int?>(null) }

    fun updateEntry(id: Int, change: (ReceiptEntry) -> ReceiptEntry) {
        entries = entries.map { if (it.id == id) change(it) else it }
    }

    fun analyze(uri: Uri) {
        phase = ReceiptPhase.ANALYZING
        errorMsg = null
        scope.launch {
            val bytes = withContext(Dispatchers.IO) { loadReceiptJpeg(context, uri) }
            val text = bytes?.let { VisionService.extractText(it) }
            entries = text?.let { parseReceipt(it) }.orEmpty()
            errorMsg = when {
                bytes == null -> "사진을 불러오지 못했어요."
                text == null  -> "글자를 읽지 못했어요. 인터넷 연결을 확인하고 다시 시도해 주세요."
                entries.isEmpty() -> "식재료를 찾지 못했어요. 영수증 전체가 밝고 반듯하게 나오도록 다시 찍어보세요."
                else -> null
            }
            phase = ReceiptPhase.DONE
        }
    }

    // 영수증 글자가 작아 미리보기(저해상도) 대신 원본 해상도로 촬영
    val photoUri = remember {
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(dir, "receipt.jpg"))
    }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) analyze(photoUri)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) analyze(uri)
    }

    val selected = entries.filter { it.checked && it.name.isNotBlank() }

    // 화면 위에 겹쳐 뜨므로, 아래 화면보다 먼저 뒤로가기를 받도록 여기서 처리
    BackHandler(onBack = onClose)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        // ── 헤더 ─────────────────────────────────────────────────────────────
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
            Column {
                Text("영수증 스캔", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colors.text)
                Text("장 본 영수증으로 한 번에 입고", fontSize = 11.sp, color = colors.textMuted)
            }
        }

        // ── 촬영 / 앨범 ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReceiptSourceButton(
                colors = colors,
                modifier = Modifier.weight(1f),
                icon = "📷",
                label = if (phase == ReceiptPhase.DONE) "다시 찍기" else "영수증 촬영",
                enabled = phase != ReceiptPhase.ANALYZING,
                onClick = { takePicture.launch(photoUri) },
            )
            ReceiptSourceButton(
                colors = colors,
                modifier = Modifier.weight(1f),
                icon = "🖼️",
                label = "앨범에서 선택",
                enabled = phase != ReceiptPhase.ANALYZING,
                onClick = { pickImage.launch("image/*") },
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── 상태 / 결과 ──────────────────────────────────────────────────────
        when (phase) {
            ReceiptPhase.IDLE -> ReceiptMessage(
                colors = colors,
                modifier = Modifier.weight(1f),
                title = "영수증을 찍어주세요",
                sub = "식재료를 찾아 수량과 예상 소비기한을 채워드려요.\n입고 전에 확인하고 고칠 수 있어요.",
            )

            ReceiptPhase.ANALYZING -> Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(color = colors.accent)
                    Text("영수증 읽는 중...", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted)
                }
            }

            ReceiptPhase.DONE -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                errorMsg?.let { msg ->
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.warningSoft)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Text(msg, fontSize = 12.sp, color = Color(0xFF9B6B1F), fontWeight = FontWeight.Medium)
                        }
                    }
                }

                if (entries.isNotEmpty()) {
                    item {
                        Text(
                            "식재료 ${entries.size}개를 찾았어요 · 확인 후 입고하세요",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                    }
                }

                items(entries, key = { it.id }) { entry ->
                    ReceiptEntryRow(
                        colors = colors,
                        entry = entry,
                        onToggle = { updateEntry(entry.id) { it.copy(checked = !it.checked) } },
                        onNameChange = { name -> updateEntry(entry.id) { it.copy(name = name) } },
                        onQtyChange = { qty -> updateEntry(entry.id) { it.copy(qty = qty) } },
                        onEditExpiry = { editingDateId = entry.id },
                    )
                }

                // 인식되지 않은 식재료 직접 추가
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                            .clickable {
                                val nextId = (entries.maxOfOrNull { it.id } ?: -1) + 1
                                entries = entries + ReceiptEntry(
                                    id = nextId,
                                    name = "",
                                    qty = 1,
                                    expiry = LocalDate.now().plusDays(7),
                                    rawLine = "직접 추가",
                                )
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("+ 직접 추가", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textMuted)
                    }
                }

                item { Spacer(Modifier.height(4.dp)) }
            }
        }

        // ── 하단 버튼 ────────────────────────────────────────────────────────
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
                    .background(if (selected.isEmpty()) colors.surfaceAlt else colors.accent)
                    .clickable(enabled = selected.isNotEmpty()) { onConfirm(selected) }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (selected.isEmpty()) "입고할 항목이 없어요" else "${selected.size}개 입고",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected.isEmpty()) colors.textMuted else Color.White,
                )
            }
        }
    }

    // ── 소비기한 수정 ────────────────────────────────────────────────────────
    editingDateId?.let { id ->
        val entry = entries.firstOrNull { it.id == id }
        if (entry == null) {
            editingDateId = null
            return@let
        }
        val zone = ZoneId.of("Asia/Seoul")
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = entry.expiry.atStartOfDay(zone).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { editingDateId = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                        updateEntry(id) { it.copy(expiry = date) }
                    }
                    editingDateId = null
                }) {
                    Text("확인", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingDateId = null }) {
                    Text("취소", color = colors.textMuted)
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun ReceiptSourceButton(
    colors: EatdaColors,
    modifier: Modifier,
    icon: String,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) colors.accentSoft else colors.surfaceAlt)
            .border(1.dp, colors.accent.copy(alpha = if (enabled) 0.25f else 0.1f), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) colors.accentDeep else colors.textMuted,
        )
    }
}

@Composable
private fun ReceiptMessage(
    colors: EatdaColors,
    modifier: Modifier,
    title: String,
    sub: String,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("🧾", fontSize = 40.sp)
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.text)
            Text(
                sub,
                fontSize = 12.sp,
                color = colors.textFaint,
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ReceiptEntryRow(
    colors: EatdaColors,
    entry: ReceiptEntry,
    onToggle: () -> Unit,
    onNameChange: (String) -> Unit,
    onQtyChange: (Int) -> Unit,
    onEditExpiry: () -> Unit,
) {
    val alpha = if (entry.checked) 1f else 0.45f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, if (entry.checked) colors.border else colors.border.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 체크
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (entry.checked) colors.accent else colors.surfaceAlt)
                .border(1.dp, if (entry.checked) colors.accent else colors.border, RoundedCornerShape(7.dp))
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (entry.checked) Text("✓", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Text(
            foodEmoji[entry.name] ?: entry.name.firstOrNull()?.toString() ?: "🥫",
            fontSize = 20.sp,
        )

        // 이름 + 원문 줄
        Column(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = entry.name,
                onValueChange = onNameChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text.copy(alpha = alpha),
                ),
                decorationBox = { inner ->
                    Box {
                        if (entry.name.isEmpty()) {
                            Text("식재료 이름", fontSize = 14.sp, color = colors.textFaint)
                        }
                        inner()
                    }
                },
            )
            Text(
                entry.rawLine,
                fontSize = 10.sp,
                color = colors.textFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            // 예상 소비기한 (누르면 수정)
            Text(
                "소비기한 ${entry.expiry.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))}  ✏️",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted.copy(alpha = alpha),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onEditExpiry)
                    .padding(vertical = 2.dp),
            )
        }

        // 수량
        Row(verticalAlignment = Alignment.CenterVertically) {
            ReceiptStepButton(colors, "−", enabled = entry.qty > 1) { onQtyChange(entry.qty - 1) }
            Text(
                "${entry.qty}",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.text.copy(alpha = alpha),
                modifier = Modifier.width(28.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            ReceiptStepButton(colors, "+", enabled = entry.qty < 99) { onQtyChange(entry.qty + 1) }
        }
    }
}

@Composable
private fun ReceiptStepButton(
    colors: EatdaColors,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceAlt)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) colors.text else colors.textFaint,
        )
    }
}

// 영수증 사진을 OCR에 보낼 크기로 줄여 JPEG로 변환 (긴 변 최대 2000px)
private fun loadReceiptJpeg(context: Context, uri: Uri, maxSide: Int = 2000): ByteArray? = runCatching {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2

    val bitmap = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return@runCatching null

    ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
}.getOrNull()
