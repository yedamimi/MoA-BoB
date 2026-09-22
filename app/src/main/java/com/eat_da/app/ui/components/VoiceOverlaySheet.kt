package com.eatda.app.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eatda.app.data.foodEmoji
import com.eatda.app.data.model.FoodItem
import com.eatda.app.ui.theme.*
import com.eatda.app.util.SttService
import com.eatda.app.util.TtsService
import com.eatda.app.util.VoiceCommand
import com.eatda.app.util.VoiceCommandParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class VoicePhase {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    SPEAKING_AWAIT,  // 삭제 확인 대기 — TTS 끝나면 자동 재청취
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceOverlaySheet(
    colors: EatdaColors,
    sheetState: SheetState,
    inventory: List<FoodItem>,
    pendingDeleteItem: FoodItem?,
    pendingDeleteQty: Int? = null,
    lastVoiceResponse: String? = null,
    isAddMode: Boolean = false,            // 재고 추가 모드 여부
    addedItems: List<FoodItem> = emptyList(), // 이번 세션에 추가된 항목
    onCommand: (VoiceCommand) -> Unit,
    onDismiss: () -> Unit,
) {
    val context    = LocalContext.current
    val sttState   by SttService.state.collectAsStateWithLifecycle()
    val ttsPlaying by TtsService.isPlaying.collectAsStateWithLifecycle()

    // rememberUpdatedState로 LaunchedEffect 클로저가 항상 최신 값을 참조하게 함
    val currentIsAddMode by rememberUpdatedState(isAddMode)

    var heardText    by remember { mutableStateOf("") }
    var phase        by remember { mutableStateOf(VoicePhase.IDLE) }
    var hasMicPerm   by remember { mutableStateOf(false) }
    var hasResponded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // 음성 초기 화면으로 복귀 — 로컬 상태 리셋 (STT 재시작 안 함, 마이크 버튼 눌러야)
    fun goBack() {
        heardText    = ""
        hasResponded = false
        onCommand(VoiceCommand.Back)
        SttService.stop()
        phase = VoicePhase.IDLE
    }

    // ── 마이크 권한 ──────────────────────────────────────────────────────────
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPerm = granted
        // 권한 허용 후에도 자동 시작 안 함 — 마이크 버튼 눌러야 시작
    }

    LaunchedEffect(Unit) {
        val pm = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        hasMicPerm = (pm == android.content.pm.PackageManager.PERMISSION_GRANTED)
        if (!hasMicPerm) permLauncher.launch(Manifest.permission.RECORD_AUDIO)
        // phase 는 IDLE 유지 — 마이크 버튼 눌러야 시작
    }

    // ── STT 결과 처리 ────────────────────────────────────────────────────────
    LaunchedEffect(sttState) {
        when (val s = sttState) {
            is SttService.SttState.Listening -> phase = VoicePhase.LISTENING
            is SttService.SttState.Result -> {
                val invNames = inventory.map { it.name }

                // 후보 중 Unknown이 아닌 첫 번째 선택 — 없으면 1순위 결과 사용
                val (bestText, cmd) = s.candidates
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .firstNotNullOfOrNull { candidate ->
                        val c = VoiceCommandParser.parse(candidate, invNames, currentIsAddMode)
                        if (c !is VoiceCommand.Unknown) candidate to c else null
                    } ?: run {
                    val fallback = s.text.trim()
                    fallback to VoiceCommandParser.parse(fallback, invNames, currentIsAddMode)
                }

                heardText = bestText
                phase = VoicePhase.PROCESSING

                // Back: 음성 초기 화면으로 복귀
                if (cmd is VoiceCommand.Back) {
                    goBack()
                    return@LaunchedEffect
                }

                onCommand(cmd)
                hasResponded = true
                phase = if (cmd is VoiceCommand.DeleteFood) VoicePhase.SPEAKING_AWAIT
                else VoicePhase.SPEAKING
                SttService.reset()
            }
            is SttService.SttState.Error -> {
                SttService.reset()
                phase = VoicePhase.IDLE
                // 에러 시 자동 재시작 안 함 — 마이크 버튼 눌러야 다시 시작
            }
            else -> {}
        }
    }

    // ── TTS 종료 처리 ────────────────────────────────────────────────────────
    var prevPlaying by remember { mutableStateOf(false) }
    LaunchedEffect(ttsPlaying) {
        if (prevPlaying && !ttsPlaying) {
            when (phase) {
                VoicePhase.SPEAKING_AWAIT -> {
                    // 삭제 확인 대기 — 항상 재청취 (응/아니 기다려야 함)
                    heardText = ""
                    phase     = VoicePhase.LISTENING
                    delay(300)
                    SttService.start(context)
                }
                VoicePhase.SPEAKING -> {
                    if (currentIsAddMode) {
                        // 재고 추가 모드 — 이어서 청취
                        heardText = ""
                        phase     = VoicePhase.LISTENING
                        delay(300)
                        SttService.start(context)
                    } else {
                        // 일반 모드 — 응답 후 초기 화면으로 (마이크 버튼 눌러야 다음 질문 가능)
                        delay(200)
                        SttService.stop()
                        heardText    = ""
                        hasResponded = false
                        phase        = VoicePhase.IDLE
                    }
                }
                else -> {}
            }
        }
        prevPlaying = ttsPlaying
    }

    DisposableEffect(Unit) { onDispose { SttService.stop() } }

    // ── UI ───────────────────────────────────────────────────────────────────
    ModalBottomSheet(
        onDismissRequest = { SttService.stop(); onDismiss() },
        sheetState       = sheetState,
        containerColor   = colors.bg,
        contentColor     = colors.text,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {

            // 상단 — 이전 버튼(좌), 레이블(중앙), 끝내기 버튼(우)
            Box(modifier = Modifier.fillMaxWidth()) {
                // 이전 버튼 — 좌측 (뭔가 말한 뒤 / 추가 모드일 때만 표시)
                if (hasResponded || isAddMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.surfaceAlt)
                            .border(1.dp, colors.border, RoundedCornerShape(999.dp))
                            .clickable {
                                if (currentIsAddMode) {
                                    onCommand(VoiceCommand.ExitAddMode)
                                } else {
                                    goBack()
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text("이전", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    }
                }

                // 중앙 레이블 + 추가 모드 배지
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (isAddMode) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.accent.copy(alpha = 0.13f))
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                        ) {
                            Text(
                                "📦 재고 추가 모드",
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color      = colors.accent,
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(
                        text = when (phase) {
                            VoicePhase.IDLE           -> if (!hasMicPerm) "마이크 권한 필요" else "클릭하세요"
                            VoicePhase.LISTENING      -> if (isAddMode) "추가할 식재료를 말씀하세요..."
                            else if (hasResponded) "계속 말씀하세요..." else "듣는 중..."
                            VoicePhase.PROCESSING     -> "분석 중..."
                            VoicePhase.SPEAKING,
                            VoicePhase.SPEAKING_AWAIT -> "응답 중..."
                        },
                        fontSize   = 13.sp,
                        color      = colors.textMuted,
                        fontWeight = FontWeight.Medium,
                    )
                }

                // 끝내기 버튼 — 우측
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.surfaceAlt)
                        .border(1.dp, colors.border, RoundedCornerShape(999.dp))
                        .clickable { SttService.stop(); onDismiss() }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text("끝내기", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.text)
                }
            }

            // 마이크 버튼
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                MicButton(
                    colors = colors,
                    phase  = phase,
                    onClick = {
                        if (!hasMicPerm) {
                            permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            return@MicButton
                        }
                        when (phase) {
                            VoicePhase.IDLE -> {
                                heardText = ""
                                phase     = VoicePhase.LISTENING
                                SttService.start(context)
                            }
                            VoicePhase.LISTENING -> {
                                SttService.stop()
                                phase = VoicePhase.IDLE
                            }
                            else -> {}
                        }
                    },
                )
            }

            // 추가 모드 — 이번 세션에 추가된 항목 목록
            if (isAddMode && addedItems.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "추가된 항목 (${addedItems.size}개)",
                        fontSize   = 11.sp,
                        color      = colors.textFaint,
                        fontWeight = FontWeight.SemiBold,
                    )
                    addedItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("✅", fontSize = 13.sp)
                            Text(
                                item.name,
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color      = colors.text,
                                modifier   = Modifier.weight(1f),
                            )
                            Text(
                                "${item.qty} · ${item.expiry?.let { "${it.monthValue}/${it.dayOfMonth}" } ?: "-"}",
                                fontSize = 12.sp,
                                color    = colors.textMuted,
                            )
                        }
                    }
                }
            }

            // 사용자 말풍선 (오른쪽)
            if (heardText.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(RoundedCornerShape(14.dp, 4.dp, 14.dp, 14.dp))
                            .background(colors.accent)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(
                            "\"$heardText\"",
                            fontSize   = 14.sp,
                            color      = Color.White,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // AI 응답 말풍선 (왼쪽)
            if (lastVoiceResponse != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment     = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceAlt),
                        contentAlignment = Alignment.Center,
                    ) { Text("🤖", fontSize = 16.sp) }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .widthIn(max = 260.dp)
                            .clip(RoundedCornerShape(4.dp, 14.dp, 14.dp, 14.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(4.dp, 14.dp, 14.dp, 14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(
                            lastVoiceResponse,
                            fontSize   = 14.sp,
                            color      = colors.text,
                            lineHeight = 20.sp,
                        )
                    }
                }
            }

            // 삭제 확인 카드
            pendingDeleteItem?.let { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.dangerSoft)
                        .border(1.dp, colors.danger.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(foodEmoji[item.name] ?: "🍽️", fontSize = 28.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (pendingDeleteQty != null) "${item.name} ${pendingDeleteQty}개 삭제할까요?"
                            else "${item.name} 삭제할까요?",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color      = colors.danger,
                        )
                        Text(
                            "\"응\" 또는 \"아니\" 라고 말하세요",
                            fontSize = 11.sp,
                            color    = colors.textMuted,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }

            // 힌트 목록
            // 추가 모드: heardText/hasResponded 무관하게 즉시 표시 (모드 진입 즉시 힌트 노출)
            // 일반 모드: 처음 열었을 때만 표시
            if (pendingDeleteItem == null) {
                if (isAddMode) {
                    AddModeHintList(colors)
                } else if (!hasResponded && heardText.isEmpty()) {
                    VoiceHintList(colors)
                }
            }
        }
    }
}

// ── 마이크 버튼 ───────────────────────────────────────────────────────────────

@Composable
private fun MicButton(colors: EatdaColors, phase: VoicePhase, onClick: () -> Unit) {
    val isListening = phase == VoicePhase.LISTENING

    val pulse by rememberInfiniteTransition(label = "micPulse").animateFloat(
        initialValue  = 1f,
        targetValue   = 1.22f,
        animationSpec = infiniteRepeatable(
            tween(650, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "micPulseScale",
    )

    Box(contentAlignment = Alignment.Center) {
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.13f)),
            )
        }
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (isListening) colors.accent else colors.surface)
                .border(2.dp, if (isListening) colors.accent else colors.border, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text     = "🎤",
                fontSize = if (isListening) 26.sp else 24.sp,
            )
        }
    }
}

// ── 일반 힌트 목록 ────────────────────────────────────────────────────────────

@Composable
private fun VoiceHintList(colors: EatdaColors) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "이렇게 말해보세요",
            fontSize   = 11.sp,
            color      = colors.textFaint,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(bottom = 2.dp),
        )
        listOf(
            "재고 추가 열어줘",
            "유통기한 N일 남은 거 있어?",
            "우유 유통기한 언제야?",
            "냉장고에 계란 있어?",
            "두부 삭제해줘",
            "끝  (종료)",
        ).forEach { hint ->
            HintRow(hint, colors)
        }
    }
}

// ── 추가 모드 힌트 목록 ───────────────────────────────────────────────────────

@Composable
private fun AddModeHintList(colors: EatdaColors) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "이렇게 말해보세요",
            fontSize   = 11.sp,
            color      = colors.textFaint,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(bottom = 2.dp),
        )
        listOf(
            "사과 1개",
            "우유 2개 12월 5일",
            "두부 3개 26년 1월 20일",
            "이전  (이전 화면으로)",
            "끝  (추가 모드 종료)",
        ).forEach { hint ->
            HintRow(hint, colors)
        }
    }
}

@Composable
private fun HintRow(hint: String, colors: EatdaColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("🎤", fontSize = 13.sp)
        Text(hint, fontSize = 12.sp, color = colors.textMuted)
    }
}
