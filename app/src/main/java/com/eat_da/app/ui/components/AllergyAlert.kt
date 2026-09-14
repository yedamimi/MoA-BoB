package com.eatda.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.eatda.app.data.model.ScanResult
import com.eatda.app.ui.theme.*

@Composable
fun AllergyAlert(colors: EatdaColors, item: ScanResult, onDismiss: () -> Unit) {
    val shakeX by rememberInfiniteTransition(label = "shake").animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(100, easing = LinearEasing), RepeatMode.Reverse),
        label = "shakeX",
    )
    val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f, targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseScale",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface)
                .then(
                    if (colors.isA11y) Modifier.border(3.dp, colors.danger, RoundedCornerShape(18.dp))
                    else Modifier
                )
                .padding(24.dp)
                .graphicsLayer { translationX = shakeX },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colors.dangerSoft)
                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale },
                contentAlignment = Alignment.Center,
            ) {
                EatdaIcon(EatdaIcons.Warn, tint = colors.danger, size = 32.dp)
            }

            Spacer(Modifier.height(14.dp))
            Text("알레르기 경고", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = colors.danger)
            Spacer(Modifier.height(4.dp))
            Text("${item.name} 감지됨", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
            Spacer(Modifier.height(4.dp))
            Text(
                "등록된 알레르기 식재료입니다.\n진동 · 음성 안내가 함께 발생했습니다.",
                fontSize = 13.sp,
                color = colors.textMuted,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    EatdaIcon(EatdaIcons.Volume, tint = colors.textFaint, size = 12.dp)
                    Text("음성", fontSize = 11.sp, color = colors.textFaint)
                }
                Text("·", fontSize = 11.sp, color = colors.textFaint)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    EatdaIcon(EatdaIcons.Bell, tint = colors.textFaint, size = 12.dp)
                    Text("진동", fontSize = 11.sp, color = colors.textFaint)
                }
            }

            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.danger)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("확인했습니다", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
