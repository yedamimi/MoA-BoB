package com.eatda.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eatda.app.ui.theme.*
import kotlinx.coroutines.delay

private data class ChatMessage(val who: String, val text: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceOverlaySheet(
    colors: EatdaColors,
    sheetState: SheetState,
    onDismiss: () -> Unit,
) {
    val messages = listOf(
        ChatMessage("user", "아래 칸 뭐 있어?"),
        ChatMessage("app",  "냉장 3칸에 딸기 1팩, 사과 4개가 있습니다."),
        ChatMessage("user", "곧 상하는 거 알려줘"),
        ChatMessage("app",  "두부는 오늘까지, 닭가슴살은 내일까지입니다."),
    )
    var visible by remember { mutableStateOf(listOf<ChatMessage>()) }

    val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f, targetValue = 0.94f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseScale",
    )

    LaunchedEffect(Unit) {
        messages.forEach {
            delay(1200)
            visible = visible + it
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp).clip(CircleShape).background(colors.accentSoft)
                        .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale },
                    contentAlignment = Alignment.Center,
                ) {
                    EatdaIcon(EatdaIcons.Mic, tint = colors.accent, size = 18.dp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("잇다 음성 비서", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    Text("Whisper · Google STT/TTS", fontSize = 11.sp, color = colors.textMuted)
                }
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(colors.surfaceAlt).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    EatdaIcon(EatdaIcons.Close, tint = colors.text, size = 16.dp)
                }
            }

            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible) { msg ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (msg.who == "user") Alignment.CenterEnd else Alignment.CenterStart,
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 260.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (msg.who == "user") colors.accent else colors.surfaceAlt)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Text(msg.text, fontSize = 13.sp, color = if (msg.who == "user") Color.White else colors.text, lineHeight = 20.sp)
                        }
                    }
                }
            }
        }
    }
}
