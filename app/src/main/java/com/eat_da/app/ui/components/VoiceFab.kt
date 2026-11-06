package com.eatda.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eatda.app.ui.theme.EatdaColors

@Composable
fun VoiceFab(
    colors: EatdaColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val pulse by rememberInfiniteTransition(label = "voicePulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "voicePulseScale",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.14f)),
        )
        Column(
            modifier = Modifier
                .size(52.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(colors.accent)
                .clickable(onClick = onClick)
                .semantics { contentDescription = "음성" },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Figma의 흰색 마이크 아이콘을 외부 아이콘 의존성 없이 그림
            Canvas(modifier = Modifier.size(19.dp)) {
                val w = size.width
                val h = size.height
                val line = 1.8.dp.toPx()
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(w * .38f, h * .08f),
                    size = androidx.compose.ui.geometry.Size(w * .24f, h * .50f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .12f),
                    style = Stroke(width = line),
                )
                drawArc(Color.White, 0f, 180f, false,
                    topLeft = Offset(w * .20f, h * .27f),
                    size = androidx.compose.ui.geometry.Size(w * .60f, h * .52f),
                    style = Stroke(width = line))
                drawLine(Color.White, Offset(w * .50f, h * .79f), Offset(w * .50f, h * .94f), line)
                drawLine(Color.White, Offset(w * .34f, h * .94f), Offset(w * .66f, h * .94f), line)
            }
            Text("음성", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
