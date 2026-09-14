package com.eatda.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eatda.app.ui.components.EatdaIcon
import com.eatda.app.ui.components.EatdaIcons
import com.eatda.app.ui.theme.EatdaColors
import com.eatda.app.util.Household
import com.eatda.app.util.HouseholdManager
import com.eatda.app.util.HouseholdMember

@Composable
fun HouseholdScreen(
    colors: EatdaColors,
    household: Household?,
    members: List<HouseholdMember>,
    myUserId: String,
    onCreateHousehold: (name: String, myName: String) -> Unit,
    onJoinHousehold: (code: String, myName: String) -> Unit,
    onLeaveHousehold: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val savedName = remember { HouseholdManager.getMyName(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.bg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
    ) {
        // ── 헤더 ─────────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceAlt)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    EatdaIcon(EatdaIcons.ChevronLeft, tint = colors.text, size = 20.dp)
                }
                Column {
                    Text("공동 냉장고", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = colors.text)
                    Text(
                        if (household != null) "구성원 ${members.size}명과 함께 관리 중"
                        else "가족·동거인과 재고를 함께 관리해요",
                        fontSize = 12.sp,
                        color    = colors.textMuted,
                    )
                }
            }
        }

        if (household == null) {
            // ── 미가입 상태 ───────────────────────────────────────────────────
            item {
                NoHouseholdContent(
                    colors    = colors,
                    savedName = savedName,
                    onCreate  = onCreateHousehold,
                    onJoin    = onJoinHousehold,
                )
            }
        } else {
            // ── 가입된 상태 ───────────────────────────────────────────────────
            item {
                HouseholdInfoCard(colors, household)
                Spacer(Modifier.height(16.dp))
            }

            item {
                Text(
                    "구성원",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color      = colors.textMuted,
                    modifier   = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                )
            }

            items(members) { member ->
                MemberRow(colors, member, isMe = member.uid == myUserId)
                Spacer(Modifier.height(6.dp))
            }

            item {
                Spacer(Modifier.height(20.dp))
                LeaveButton(colors, onLeave = onLeaveHousehold)
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ── 미가입 콘텐츠 ─────────────────────────────────────────────────────────────

@Composable
private fun NoHouseholdContent(
    colors: EatdaColors,
    savedName: String,
    onCreate: (name: String, myName: String) -> Unit,
    onJoin: (code: String, myName: String) -> Unit,
) {
    var mode    by remember { mutableStateOf<String?>(null) }   // "create" | "join"
    var myName  by remember { mutableStateOf(savedName) }
    var input   by remember { mutableStateOf("") }              // 그룹명 or 초대코드
    var loading by remember { mutableStateOf(false) }

    // 소개 카드
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.accentSoft)
            .border(1.dp, colors.accent.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("🏠", fontSize = 36.sp)
        Text("공동 냉장고 관리", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = colors.accentDeep)
        Text(
            "가족·동거인을 초대해서\n냉장고 재고를 함께 관리해 보세요",
            fontSize   = 13.sp,
            color      = colors.accentDeep.copy(alpha = 0.7f),
            textAlign  = TextAlign.Center,
            lineHeight = 20.sp,
        )
    }

    Spacer(Modifier.height(20.dp))

    // 이름 입력 (공통)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("내 이름", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted, modifier = Modifier.padding(start = 4.dp))
        OutlinedTextField(
            value         = myName,
            onValueChange = { myName = it },
            placeholder   = { Text("이름 또는 닉네임") },
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(12.dp),
            singleLine    = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = colors.accent,
                unfocusedBorderColor = colors.border,
                focusedTextColor     = colors.text,
                unfocusedTextColor   = colors.text,
            ),
        )
    }

    Spacer(Modifier.height(14.dp))

    // 탭 선택: 새로 만들기 / 코드 입력
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceAlt)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf("create" to "새로 만들기", "join" to "코드로 참여").forEach { (key, label) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (mode == key) colors.accent else Color.Transparent)
                    .clickable { mode = key; input = "" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (mode == key) Color.White else colors.textMuted,
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    AnimatedVisibility(visible = mode != null) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val label       = if (mode == "create") "냉장고 이름" else "초대 코드 6자리"
            val placeholder = if (mode == "create") "예) 우리집 냉장고" else "예) ABC123"
            val btnLabel    = if (mode == "create") "만들기" else "참여하기"

            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted, modifier = Modifier.padding(start = 4.dp))
            OutlinedTextField(
                value         = input,
                onValueChange = { input = it.uppercase().take(if (mode == "join") 6 else 30) },
                placeholder   = { Text(placeholder) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction      = ImeAction.Done,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = colors.accent,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor     = colors.text,
                    unfocusedTextColor   = colors.text,
                ),
            )

            val canSubmit = myName.isNotBlank() && input.isNotBlank() &&
                    (mode == "create" || input.length == 6)

            Button(
                onClick = {
                    if (loading || !canSubmit) return@Button
                    loading = true
                    if (mode == "create") onCreate(input.trim(), myName.trim())
                    else                  onJoin(input.trim(), myName.trim())
                },
                enabled  = canSubmit && !loading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = colors.accent),
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(btnLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// ── 가입된 상태: 가구 정보 카드 ──────────────────────────────────────────────

@Composable
private fun HouseholdInfoCard(colors: EatdaColors, household: Household) {
    var codeCopied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(colors.accentSoft),
                contentAlignment = Alignment.Center,
            ) { Text("🏠", fontSize = 22.sp) }
            Column(modifier = Modifier.weight(1f)) {
                Text(household.name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = colors.text)
                Text("공동 냉장고", fontSize = 11.sp, color = colors.textMuted)
            }
        }

        // 초대 코드
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("초대 코드", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceAlt)
                    .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    household.inviteCode.chunked(3).joinToString(" – "),
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    color      = colors.accent,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (codeCopied) colors.accentSoft else colors.accent)
                        .clickable { codeCopied = true }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        if (codeCopied) "복사됨 ✓" else "복사",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (codeCopied) colors.accentDeep else Color.White,
                    )
                }
            }
            Text("이 코드를 공유하면 구성원을 초대할 수 있어요", fontSize = 11.sp, color = colors.textFaint)
        }
    }
}

// ── 구성원 행 ─────────────────────────────────────────────────────────────────

@Composable
private fun MemberRow(colors: EatdaColors, member: HouseholdMember, isMe: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) { Text(member.emoji, fontSize = 20.sp) }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(member.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
                if (isMe) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.accentSoft)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) { Text("나", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.accentDeep) }
                }
            }
            if (member.isOwner) {
                Text("방장", fontSize = 11.sp, color = colors.textMuted)
            }
        }

        if (isMe) {
            EatdaIcon(EatdaIcons.ChevronRight, tint = colors.textFaint, size = 16.dp)
        }
    }
}

// ── 나가기 버튼 ───────────────────────────────────────────────────────────────

@Composable
private fun LeaveButton(colors: EatdaColors, onLeave: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.dangerSoft)
            .border(1.dp, colors.danger.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable { showConfirm = true }
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("공동 냉장고에서 나가기", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.danger)
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title            = { Text("공동 냉장고 나가기") },
            text             = { Text("나가면 다른 구성원의 재고를 더 이상 볼 수 없어요.\n다시 참여하려면 초대 코드가 필요해요.") },
            confirmButton    = {
                TextButton(onClick = { showConfirm = false; onLeave() }) {
                    Text("나가기", color = colors.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("취소") }
            },
        )
    }
}
