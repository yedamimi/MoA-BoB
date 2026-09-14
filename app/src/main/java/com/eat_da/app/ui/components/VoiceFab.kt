package com.eatda.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
        initialValue  = 1f,
        targetValue   = 1.10f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "voicePulseScale",
    )

    Box(
        modifier         = modifier,
        contentAlignment = Alignment.Center,
    ) {
        // 바깥 펄스 링
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.14f)),
        )
        // 버튼 본체
        Column(
            modifier = Modifier
                .size(52.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(colors.accent)
                .clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("🎤", fontSize = 18.sp)
            Text(
                "음성검색",
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.sp,
            )
        }
    }
}
