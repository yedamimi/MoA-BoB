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
import androidx.compose.ui.text.style.TextAlign
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
    onCommand: (VoiceCommand) -> Unit,
    onDismiss: () -> Unit,
) {
    val context     = LocalContext.current
    val sttState    by SttService.state.collectAsStateWithLifecycle()
    val ttsPlaying  by TtsService.isPlaying.collectAsStateWithLifecycle()

    var heardText   by remember { mutableStateOf("") }
    var phase       by remember { mutableStateOf(VoicePhase.IDLE) }
    var hasMicPerm  by remember { mutableStateOf(false) }

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
                heardText = s.text
                phase     = VoicePhase.PROCESSING
                val cmd   = VoiceCommandParser.parse(s.text, inventory.map { it.name })
                onCommand(cmd)
                phase = if (cmd is VoiceCommand.DeleteFood) VoicePhase.SPEAKING_AWAIT
                else VoicePhase.SPEAKING
                SttService.reset()
            }
            is SttService.SttState.Error -> {
                phase = VoicePhase.IDLE
                SttService.reset()
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
                    heardText = ""
                    phase     = VoicePhase.LISTENING
                    SttService.start(context)
                }
                VoicePhase.SPEAKING -> phase = VoicePhase.IDLE
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
            // 상태 레이블
            Text(
                text = when (phase) {
                    VoicePhase.IDLE           -> if (!hasMicPerm) "마이크 권한이 필요합니다" else "마이크를 탭해 시작"
                    VoicePhase.LISTENING      -> "듣는 중..."
                    VoicePhase.PROCESSING     -> "분석 중..."
                    VoicePhase.SPEAKING       -> "응답 중..."
                    VoicePhase.SPEAKING_AWAIT -> "응답 후 다시 듣겠습니다"
                },
                fontSize   = 13.sp,
                color      = colors.textMuted,
                fontWeight = FontWeight.Medium,
            )

            // 마이크 버튼
            MicButton(
                colors    = colors,
                listening = phase == VoicePhase.LISTENING,
                onClick   = {
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

            // 인식된 텍스트
            if (heardText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                ) {
                    Text(
                        "\"$heardText\"",
                        fontSize   = 15.sp,
                        color      = colors.text,
                        fontWeight = FontWeight.Medium,
                        textAlign  = TextAlign.Center,
                        modifier   = Modifier.fillMaxWidth(),
                    )
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
                    verticalAlignment    = Alignment.CenterVertically,
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

            // 힌트 목록 (대기/청취 중 — 아직 말하기 전)
            if (phase in listOf(VoicePhase.IDLE, VoicePhase.LISTENING) && heardText.isEmpty() && pendingDeleteItem == null) {
                VoiceHintList(colors)
            }
        }
    }
}

@Composable
private fun MicButton(colors: EatdaColors, listening: Boolean, onClick: () -> Unit) {
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
        if (listening) {
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
                .background(if (listening) colors.accent else colors.surface)
                .border(2.dp, if (listening) colors.accent else colors.border, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text     = "🎤",
                fontSize = if (listening) 26.sp else 24.sp,
                color    = Color.Unspecified,
            )
        }
    }
}

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
            "우유 유통기한 언제야?",
            "냉장고에 계란 있어?",
            "곧 상하는 음식 알려줘",
            "두부 삭제해줘",
        ).forEach { hint ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("🎤", fontSize = 13.sp)
                Text(hint, fontSize = 12.sp, color = colors.textMuted)
            }
        }
    }
}
