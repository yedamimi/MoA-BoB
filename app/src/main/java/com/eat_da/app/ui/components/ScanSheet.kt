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
    onPickAllergen: () -> Unit,
    onDismiss: () -> Unit,
    a11yMode: Boolean = false,
) {
    val titleSize = if (a11yMode) 24.sp else 19.sp
    val descriptionSize = if (a11yMode) 16.sp else 12.sp
    val rowTitleSize = if (a11yMode) 19.sp else 15.sp
    val rowDescriptionSize = if (a11yMode) 15.sp else 11.sp
    val cancelSize = if (a11yMode) 18.sp else 14.sp

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
                .padding(bottom = if (a11yMode) 28.dp else 22.dp),
            verticalArrangement = Arrangement.spacedBy(if (a11yMode) 16.dp else 12.dp),
        ) {
            Text(
                "어떤 스캔을 할까요?",
                fontSize = titleSize,
                color = colors.text,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Text(
                "원하는 작업을 선택해 주세요",
                fontSize = descriptionSize,
                color = colors.textMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (a11yMode) 8.dp else 6.dp),
                textAlign = TextAlign.Center,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (a11yMode) 10.dp else 8.dp),
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
                    a11yMode = a11yMode,
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
                    a11yMode = a11yMode,
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
                    a11yMode = a11yMode,
                    onClick = { onPick(ScanMode.FRESH) },
                )
            }

            ScanMenuRow(
                colors = colors,
                icon = "🧾",
                title = "영수증 스캔",
                description = "장 본 영수증으로 한 번에 입고",
                a11yMode = a11yMode,
                onClick = onPickReceipt,
            )

            ScanMenuRow(
                colors = colors,
                icon = "⚠️",
                title = "성분표 스캔",
                description = "알레르기 유발 성분 확인 · 제품 뒷면 성분표를 찍어보세요",
                a11yMode = a11yMode,
                onClick = onPickAllergen,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (a11yMode) 58.dp else 46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = if (a11yMode) 18.dp else 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "취소",
                    fontSize = cancelSize,
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                )
            }
        }
    }
}

@Composable
private fun ScanMenuRow(
    colors: EatdaColors,
    icon: String,
    title: String,
    description: String,
    a11yMode: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (a11yMode) 82.dp else 66.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (a11yMode) 18.dp else 16.dp,
                vertical = if (a11yMode) 20.dp else 16.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (a11yMode) 16.dp else 12.dp),
    ) {
        Text(icon, fontSize = if (a11yMode) 32.sp else 26.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = if (a11yMode) 19.sp else 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.text,
            )
            Text(
                description,
                fontSize = if (a11yMode) 15.sp else 11.sp,
                color = colors.textMuted,
                fontWeight = FontWeight.Medium,
                lineHeight = if (a11yMode) 21.sp else 15.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        EatdaIcon(
            EatdaIcons.ChevronRight,
            tint = colors.textMuted,
            size = if (a11yMode) 24.dp else 18.dp,
        )
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
    a11yMode: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .heightIn(min = if (a11yMode) 142.dp else 112.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (a11yMode) 5.dp else 4.dp,
                vertical = if (a11yMode) 22.dp else 20.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (a11yMode) 10.dp else 8.dp),
    ) {
        EatdaIcon(icon, tint = iconColor, size = if (a11yMode) 34.dp else 28.dp)
        Text(
            title,
            fontSize = if (a11yMode) 20.sp else 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = titleColor,
        )
        Text(
            sub,
            fontSize = if (a11yMode) 13.sp else 10.sp,
            lineHeight = if (a11yMode) 17.sp else 13.sp,
            color = subColor,
            fontWeight = FontWeight.Medium,
            maxLines = if (a11yMode) 2 else 1,
            textAlign = TextAlign.Center,
        )
    }
}
