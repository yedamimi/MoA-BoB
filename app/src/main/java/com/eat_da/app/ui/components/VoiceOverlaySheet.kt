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

private enum class VoicePhase {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    SPEAKING_AWAIT,  // 삭제 확인 대기 — TTS 끝나면 자동 재청취
}

/** 이 키워드가 인식되면 음성 오버레이를 닫는다 */
private val STOP_KEYWORDS = setOf(
    "끝", "종료", "그만", "닫아", "끝내", "나가", "그만해", "종료해", "닫기", "없애"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceOverlaySheet(
    colors: EatdaColors,
    sheetState: SheetState,
    inventory: List<FoodItem>,
    pendingDeleteItem: FoodItem?,
    lastVoiceResponse: String? = null,
    onCommand: (VoiceCommand) -> Unit,
    onDismiss: () -> Unit,
) {
    val context    = LocalContext.current
    val sttState   by SttService.state.collectAsStateWithLifecycle()
    val ttsPlaying by TtsService.isPlaying.collectAsStateWithLifecycle()

    var heardText    by remember { mutableStateOf("") }
    var phase        by remember { mutableStateOf(VoicePhase.IDLE) }
    var hasMicPerm   by remember { mutableStateOf(false) }
    var hasResponded by remember { mutableStateOf(false) }

    // ── 마이크 권한 ──────────────────────────────────────────────────────────
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPerm = granted
        if (granted) { phase = VoicePhase.LISTENING; SttService.start(context) }
    }

    LaunchedEffect(Unit) {
        val pm = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        hasMicPerm = (pm == android.content.pm.PackageManager.PERMISSION_GRANTED)
        if (hasMicPerm) { phase = VoicePhase.LISTENING; SttService.start(context) }
        else permLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // ── STT 결과 처리 ────────────────────────────────────────────────────────
    LaunchedEffect(sttState) {
        when (val s = sttState) {
            is SttService.SttState.Listening -> phase = VoicePhase.LISTENING
            is SttService.SttState.Result -> {
                val text = s.text.trim()
                heardText = text

                // 종료 키워드 감지 → 즉시 닫기
                if (STOP_KEYWORDS.any { text.contains(it) }) {
                    SttService.stop()
                    SttService.reset()
                    onDismiss()
                    return@LaunchedEffect
                }

                phase = VoicePhase.PROCESSING
                val cmd = VoiceCommandParser.parse(text, inventory.map { it.name })
                onCommand(cmd)
                hasResponded = true
                phase = if (cmd is VoiceCommand.DeleteFood) VoicePhase.SPEAKING_AWAIT
                else VoicePhase.SPEAKING
                SttService.reset()
            }
            is SttService.SttState.Error -> {
                // reset() 을 먼저 호출하면 sttState 키가 바뀌어 이 코루틴이 취소됨
                // → reset() 생략하고 start() 내부에서 stop() 처리하도록
                if (hasMicPerm) {
                    phase = VoicePhase.LISTENING
                    delay(600)
                    SttService.start(context)   // 내부에서 stop() → 새 recognizer 생성
                } else {
                    phase = VoicePhase.IDLE
                    SttService.reset()
                }
            }
            else -> {}
        }
    }

    // ── TTS 종료 처리 ────────────────────────────────────────────────────────
    var prevPlaying by remember { mutableStateOf(false) }
    LaunchedEffect(ttsPlaying) {
        if (prevPlaying && !ttsPlaying) {
            when (phase) {
                VoicePhase.SPEAKING_AWAIT,
                VoicePhase.SPEAKING -> {
                    // TTS 종료 → 바로 재청취 (300ms 후 — 에코 방지)
                    heardText = ""
                    phase     = VoicePhase.LISTENING
                    delay(300)
                    SttService.start(context)
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

            // 상단 — 레이블 중앙 / 끝내기 버튼 우측 절대 위치
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = when (phase) {
                        VoicePhase.IDLE           -> if (!hasMicPerm) "마이크 권한 필요" else "잠시만요..."
                        VoicePhase.LISTENING      -> if (hasResponded) "계속 말씀하세요..." else "듣는 중..."
                        VoicePhase.PROCESSING     -> "분석 중..."
                        VoicePhase.SPEAKING,
                        VoicePhase.SPEAKING_AWAIT -> "응답 중..."
                    },
                    fontSize   = 13.sp,
                    color      = colors.textMuted,
                    fontWeight = FontWeight.Medium,
                    modifier   = Modifier.align(Alignment.Center),
                )
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

            // 마이크 버튼 — 가운데 정렬
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                MicButton(
                    colors = colors,
                    phase  = phase,
                    onClick = {
                        if (!hasMicPerm) {
                            permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            return@MicButton
                        }
                        if (phase == VoicePhase.IDLE) {
                            heardText = ""
                            phase     = VoicePhase.LISTENING
                            SttService.start(context)
                        }
                    },
                )
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
                            "${item.name} 삭제할까요?",
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

            // 힌트 목록 (처음 열었을 때만)
            if (!hasResponded && heardText.isEmpty() && pendingDeleteItem == null) {
                VoiceHintList(colors)
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

// ── 힌트 목록 ─────────────────────────────────────────────────────────────────

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
            "유통기한 N일 남은 거 있어?",
            "우유 유통기한 언제야?",
            "냉장고에 계란 있어?",
            "두부 삭제해줘",
            "끝  (종료)",
        ).forEach { hint ->
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
    }
}
