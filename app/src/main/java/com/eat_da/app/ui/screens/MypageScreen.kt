package com.eatda.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eatda.app.data.ALLERGEN_PRESETS
import com.eatda.app.data.allergenPreset
import com.eatda.app.data.foodEmoji
import com.eatda.app.data.model.Allergen
import com.eatda.app.ui.components.*
import com.eatda.app.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyPageScreen(
    colors: EatdaColors,
    sizes: EatdaSizes,
    allergens: List<Allergen>,
    onRemoveAllergen: (String) -> Unit,
    onAddAllergen: (String) -> Unit,
    onGoHousehold: () -> Unit = {},
) {
    var allergenInput by remember { mutableStateOf("") }

    fun submitAllergen() {
        val name = allergenInput.trim()
        if (name.isNotEmpty()) onAddAllergen(name)
        allergenInput = ""
    }

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

        // ── 알레르기 등록: 직접 입력 + 대표 항목 선택 ──────────────────────────
        item {
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.dangerSoft).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EatdaIcon(EatdaIcons.Warn, tint = colors.danger, size = 20.dp)
                    Text("현재 ${allergens.size}개 등록됨", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.danger)
                }

                // 직접 입력
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BasicTextField(
                        value = allergenInput,
                        onValueChange = { allergenInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, color = colors.text),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitAllergen() }),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                                if (allergenInput.isEmpty()) {
                                    Text("식재료 이름 입력 (예: 키위)", fontSize = 14.sp, color = colors.textFaint)
                                }
                                innerTextField()
                            }
                        },
                    )
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (allergenInput.isNotBlank()) colors.danger else colors.danger.copy(alpha = 0.4f))
                            .clickable(enabled = allergenInput.isNotBlank()) { submitAllergen() }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("추가", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // 대표 항목: 누르면 등록, 등록된 항목을 다시 누르면 해제
                Text("자주 등록하는 항목", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.danger)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ALLERGEN_PRESETS.forEach { preset ->
                        val registered = allergens.firstOrNull { it.name == preset.name }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (registered != null) colors.danger else colors.surface)
                                .border(1.dp, if (registered != null) colors.danger else colors.border, RoundedCornerShape(999.dp))
                                .clickable {
                                    if (registered != null) onRemoveAllergen(registered.id)
                                    else onAddAllergen(preset.name)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                "${if (registered != null) "✓" else preset.emoji} ${preset.name}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (registered != null) Color.White else colors.text,
                            )
                        }
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
                        val preset = allergenPreset(a.name)
                        if (i > 0) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(colors.dangerSoft), contentAlignment = Alignment.Center) {
                                Text(
                                    foodEmoji[a.name] ?: preset?.emoji ?: a.name.firstOrNull()?.toString() ?: "?",
                                    fontSize = 18.sp,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(a.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
                                // 대표 항목은 함께 경고하는 식재료를 보여줌
                                val related = preset?.aliases?.filter { it != a.name }.orEmpty()
                                if (related.isNotEmpty()) {
                                    Text(
                                        "${related.take(4).joinToString("·")}${if (related.size > 4) " 등" else ""}도 경고",
                                        fontSize = 11.sp,
                                        color = colors.textMuted,
                                        modifier = Modifier.padding(top = 1.dp),
                                    )
                                }
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
