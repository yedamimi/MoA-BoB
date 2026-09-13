package com.eatda.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eatda.app.data.foodEmoji
import com.eatda.app.data.model.Allergen
import com.eatda.app.ui.components.*
import com.eatda.app.ui.theme.*

@Composable
fun MyPageScreen(
    colors: EatdaColors,
    sizes: EatdaSizes,
    allergens: List<Allergen>,
    onRemoveAllergen: (String) -> Unit,
    onScanAllergen: () -> Unit,
    onGoHousehold: () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.bg),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        item { Text("마이페이지", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = colors.text, modifier = Modifier.padding(horizontal = 4.dp, vertical = 14.dp)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).border(1.dp, colors.border, RoundedCornerShape(16.dp)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(Brush.linearGradient(listOf(colors.accent, colors.accentDeep))),
                    contentAlignment = Alignment.Center,
                ) { Text("길", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
                Column(modifier = Modifier.weight(1f)) {
                    Text("길민재", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    Text("구성원 1인", fontSize = 12.sp, color = colors.textMuted, modifier = Modifier.padding(top = 2.dp))
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(colors.surface).border(1.dp, colors.border, RoundedCornerShape(999.dp)).padding(horizontal = 12.dp, vertical = 6.dp).clickable { }) {
                    Text("편집", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // ── 공동 냉장고 배너 ─────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accentSoft)
                    .border(1.dp, colors.accent.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onGoHousehold)
                    .padding(14.dp),
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(colors.accent),
                    contentAlignment = Alignment.Center,
                ) { Text("🏠", fontSize = 20.sp) }
                Column(modifier = Modifier.weight(1f)) {
                    Text("공동 냉장고", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = colors.accentDeep)
                    Text("가족·동거인과 재고 함께 관리", fontSize = 11.sp, color = colors.accent.copy(alpha = 0.7f), modifier = Modifier.padding(top = 2.dp))
                }
                EatdaIcon(EatdaIcons.ChevronRight, tint = colors.accent, size = 18.dp)
            }
            Spacer(Modifier.height(14.dp))
        }

        item { SectionHeader(colors, sizes, "알레르기 식재료", "입출고 시 자동 경고") }

        item {
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.dangerSoft).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EatdaIcon(EatdaIcons.Warn, tint = colors.danger, size = 20.dp)
                    Text("현재 ${allergens.size}개 등록됨", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.danger)
                }
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.danger).clickable(onClick = onScanAllergen).padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        EatdaIcon(EatdaIcons.Camera, tint = Color.White, size = 18.dp)
                        Text("알레르기 식재료 스캔하여 등록", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        item {
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surface).border(1.dp, colors.border, RoundedCornerShape(14.dp))) {
                if (allergens.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("등록된 알레르기 식재료가 없습니다", fontSize = 13.sp, color = colors.textMuted)
                    }
                } else {
                    allergens.forEachIndexed { i, a ->
                        if (i > 0) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(colors.dangerSoft), contentAlignment = Alignment.Center) {
                                Text(foodEmoji[a.name] ?: a.name.first().toString(), fontSize = 18.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(a.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
                                Text("${a.addedAt} 등록", fontSize = 11.sp, color = colors.textFaint, modifier = Modifier.padding(top = 1.dp))
                            }
                            Box(modifier = Modifier.size(32.dp).clickable { onRemoveAllergen(a.id) }, contentAlignment = Alignment.Center) {
                                EatdaIcon(EatdaIcons.Trash, tint = colors.textMuted, size = 18.dp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
