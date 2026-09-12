package com.eatda.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eatda.app.data.model.ScanMode
import com.eatda.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanBottomSheet(
    colors: EatdaColors,
    sheetState: SheetState,
    onPick: (ScanMode) -> Unit,
    onPhonePick: (ScanMode) -> Unit,
    onDismiss: () -> Unit,
) {
    var useCamera by remember { mutableStateOf(false) }
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.text,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 14.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.borderStrong),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "어떤 스캔을 할까요?",
                fontSize = 14.sp,
                color = colors.textMuted,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceAlt)
                    .padding(3.dp),
            ) {
                listOf(false to ("📷" to "냉장고 카메라"), true to ("📱" to "스마트폰 카메라")).forEach { (isCamera, pair) ->
                    val (emoji, label) = pair
                    val selected = useCamera == isCamera
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) colors.surface else Color.Transparent)
                            .border(
                                if (selected) 1.dp else 0.dp,
                                if (selected) colors.border else Color.Transparent,
                                RoundedCornerShape(10.dp),
                            )
                            .clickable { useCamera = isCamera }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(emoji, fontSize = 13.sp)
                            Text(
                                label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) colors.text else colors.textMuted,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ScanOption(
                    modifier = Modifier.weight(1f),
                    bg = colors.accentSoft,
                    border = colors.accent.copy(alpha = 0.2f),
                    icon = EatdaIcons.Package,
                    iconColor = colors.accent,
                    title = "입고",
                    sub = "냉장고에 넣기",
                    titleColor = colors.accentDeep,
                    subColor = colors.textMuted,
                    onClick = {
                        if (useCamera) {
                            onPhonePick(ScanMode.IN)
                        } else {
                            onPick(ScanMode.IN)
                        }
                    },
                )
                ScanOption(
                    modifier = Modifier.weight(1f),
                    bg = Color(0xFFFBE9D9),
                    border = Color(0xFFE88937).copy(alpha = 0.33f),
                    icon = EatdaIcons.Package,
                    iconColor = Color(0xFFD17231),
                    title = "출고",
                    sub = "냉장고에서 꺼내기",
                    titleColor = Color(0xFF9B5021),
                    subColor = colors.textMuted,
                    onClick = {
                        if (useCamera) {
                            onPhonePick(ScanMode.OUT)
                        } else {
                            onPick(ScanMode.OUT)
                        }
                    },
                )
                ScanOption(
                    modifier = Modifier.weight(1f),
                    bg = Color(0xFFECEAF8),
                    border = Color(0xFF5A4FB5).copy(alpha = 0.2f),
                    icon = EatdaIcons.Leaf,
                    iconColor = Color(0xFF5A4FB5),
                    title = "신선도",
                    sub = "부패 확인",
                    titleColor = Color(0xFF3B3287),
                    subColor = colors.textMuted,
                    onClick = { if (useCamera) onPhonePick(ScanMode.FRESH) else onPick(ScanMode.FRESH) },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("취소", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
            }
        }
    }
}

@Composable
private fun ScanOption(
    modifier: Modifier,
    bg: Color,
    border: Color,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    sub: String,
    titleColor: Color,
    subColor: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        EatdaIcon(icon, tint = iconColor, size = 30.dp)
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = titleColor)
        Text(sub, fontSize = 10.sp, color = subColor, fontWeight = FontWeight.Medium)
    }
}