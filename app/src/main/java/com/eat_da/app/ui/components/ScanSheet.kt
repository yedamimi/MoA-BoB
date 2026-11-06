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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    onPickReceipt: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.text,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 14.dp, bottom = 8.dp)
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
                fontSize = 19.sp,
                color = colors.text,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Text(
                "원하는 작업을 선택해 주세요",
                fontSize = 12.sp,
                color = colors.textMuted,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                textAlign = TextAlign.Center,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ScanOption(
                    modifier = Modifier.weight(1f),
                    bg = colors.accentSoft,
                    border = colors.accent.copy(alpha = 0.24f),
                    icon = EatdaIcons.Package,
                    iconColor = colors.accent,
                    title = "입고",
                    sub = "냉장고에 넣기",
                    titleColor = colors.accentDeep,
                    subColor = colors.textMuted,
                    onClick = { onPick(ScanMode.IN) },
                )
                ScanOption(
                    modifier = Modifier.weight(1f),
                    bg = Color(0xFFFFF0E4),
                    border = Color(0xFFF1CEB1),
                    icon = EatdaIcons.Package,
                    iconColor = Color(0xFFC16C2E),
                    title = "출고",
                    sub = "냉장고에서 꺼내기",
                    titleColor = Color(0xFFA85A25),
                    subColor = Color(0xFF936F57),
                    onClick = { onPick(ScanMode.OUT) },
                )
                ScanOption(
                    modifier = Modifier.weight(1f),
                    bg = Color(0xFFEFEBFA),
                    border = Color(0xFFD4CAEF),
                    icon = EatdaIcons.Leaf,
                    iconColor = Color(0xFF6654B6),
                    title = "신선도",
                    sub = "부패 확인",
                    titleColor = Color(0xFF57469B),
                    subColor = Color(0xFF786D9D),
                    onClick = { onPick(ScanMode.FRESH) },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                    .clickable(onClick = onPickReceipt)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("🧾", fontSize = 26.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text("영수증 스캔", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = colors.text)
                    Text(
                        "장 본 영수증으로 한 번에 입고",
                        fontSize = 11.sp,
                        color = colors.textMuted,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                EatdaIcon(EatdaIcons.ChevronRight, tint = colors.textMuted, size = 18.dp)
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
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EatdaIcon(icon, tint = iconColor, size = 28.dp)
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = titleColor)
        Text(sub, fontSize = 10.sp, color = subColor, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
