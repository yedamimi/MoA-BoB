package com.eatda.app.util

import android.content.Context
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.UUID

/**
 * Firebase 구조:
 * households/
 *   <householdId>/
 *     name: String
 *     ownerId: String
 *     inviteCode: String          ← 6자리 초대 코드
 *     members/
 *       <userId>/
 *         name: String
 *         emoji: String
 *         joinedAt: Long
 *     inventory/
 *       <itemKey>/  ← 구성원 공유 수동 재고
 *
 * inviteCodes/
 *   <code>: <householdId>         ← 코드로 가구 ID 빠르게 조회
 */
object HouseholdManager {

    private val db = Firebase.database.reference

    // ── 내 사용자 ID (기기 고유값, 앱 설치 시 1회 생성) ────────────────────────

    fun getOrCreateUserId(context: Context): String {
        val prefs = context.getSharedPreferences("eatda_prefs", Context.MODE_PRIVATE)
        return prefs.getString("userId", null) ?: run {
            val id = UUID.randomUUID().toString().replace("-", "").take(16)
            prefs.edit().putString("userId", id).apply()
            id
        }
    }

    fun getMyName(context: Context): String {
        val prefs = context.getSharedPreferences("eatda_prefs", Context.MODE_PRIVATE)
        return prefs.getString("userName", "나") ?: "나"
    }

    fun saveMyName(context: Context, name: String) {
        context.getSharedPreferences("eatda_prefs", Context.MODE_PRIVATE)
            .edit().putString("userName", name).apply()
    }

    fun getSavedHouseholdId(context: Context): String? {
        return context.getSharedPreferences("eatda_prefs", Context.MODE_PRIVATE)
            .getString("householdId", null)
    }

    fun saveHouseholdId(context: Context, id: String?) {
        context.getSharedPreferences("eatda_prefs", Context.MODE_PRIVATE)
            .edit().putString("householdId", id).apply()
    }

    // ── 가구 생성 ────────────────────────────────────────────────────────────

    fun createHousehold(
        context: Context,
        householdName: String,
        myName: String,
        onSuccess: (householdId: String, inviteCode: String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val userId      = getOrCreateUserId(context)
        val householdId = UUID.randomUUID().toString().replace("-", "").take(20)
        val inviteCode  = generateInviteCode()

        saveMyName(context, myName)

        val householdData = mapOf(
            "name"       to householdName,
            "ownerId"    to userId,
            "inviteCode" to inviteCode,
        )
        val memberData = mapOf(
            "name"     to myName,
            "emoji"    to pickEmoji(0),
            "joinedAt" to System.currentTimeMillis(),
            "isOwner"  to true,
        )

        db.child("households").child(householdId).setValue(householdData)
            .addOnSuccessListener {
                db.child("households").child(householdId).child("members").child(userId)
                    .setValue(memberData)
                db.child("inviteCodes").child(inviteCode).setValue(householdId)
                saveHouseholdId(context, householdId)
                onSuccess(householdId, inviteCode)
            }
            .addOnFailureListener { onError(it.message ?: "생성 실패") }
    }

    // ── 초대 코드로 참여 ─────────────────────────────────────────────────────

    fun joinHousehold(
        context: Context,
        inviteCode: String,
        myName: String,
        onSuccess: (householdId: String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val userId = getOrCreateUserId(context)
        saveMyName(context, myName)

        db.child("inviteCodes").child(inviteCode.uppercase())
            .get()
            .addOnSuccessListener { snap ->
                val householdId = snap.getValue(String::class.java)
                if (householdId == null) {
                    onError("올바르지 않은 초대 코드예요")
                    return@addOnSuccessListener
                }

                // 이미 구성원인지 확인
                db.child("households").child(householdId).child("members").get()
                    .addOnSuccessListener { membersSnap ->
                        val memberCount = membersSnap.childrenCount.toInt()
                        val memberData = mapOf(
                            "name"     to myName,
                            "emoji"    to pickEmoji(memberCount),
                            "joinedAt" to System.currentTimeMillis(),
                            "isOwner"  to false,
                        )
                        db.child("households").child(householdId).child("members")
                            .child(userId).setValue(memberData)
                            .addOnSuccessListener {
                                saveHouseholdId(context, householdId)
                                onSuccess(householdId)
                            }
                            .addOnFailureListener { onError(it.message ?: "참여 실패") }
                    }
            }
            .addOnFailureListener { onError("네트워크 오류가 발생했어요") }
    }

    // ── 가구에서 나가기 ──────────────────────────────────────────────────────

    fun leaveHousehold(
        context: Context,
        householdId: String,
        onDone: () -> Unit,
    ) {
        val userId = getOrCreateUserId(context)
        db.child("households").child(householdId).child("members").child(userId).removeValue()
            .addOnCompleteListener {
                saveHouseholdId(context, null)
                onDone()
            }
    }

    // ── 구성원 목록 실시간 구독 ──────────────────────────────────────────────

    fun listenToMembers(
        householdId: String,
        onChange: (List<HouseholdMember>) -> Unit,
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val members = snapshot.children.mapNotNull { snap ->
                    val name     = snap.child("name").getValue(String::class.java) ?: return@mapNotNull null
                    val emoji    = snap.child("emoji").getValue(String::class.java) ?: "👤"
                    val isOwner  = snap.child("isOwner").getValue(Boolean::class.java) ?: false
                    val joinedAt = snap.child("joinedAt").getValue(Long::class.java) ?: 0L
                    HouseholdMember(uid = snap.key ?: "", name = name, emoji = emoji, isOwner = isOwner, joinedAt = joinedAt)
                }.sortedByDescending { it.isOwner }
                onChange(members)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("households").child(householdId).child("members")
            .addValueEventListener(listener)
        return listener
    }

    fun removeListener(householdId: String, listener: ValueEventListener) {
        db.child("households").child(householdId).child("members")
            .removeEventListener(listener)
    }

    // ── 유틸 ────────────────────────────────────────────────────────────────

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun pickEmoji(index: Int): String {
        val pool = listOf("🧑", "👩", "👨", "🧒", "👧", "👦", "🧓", "👴", "👵")
        return pool[index % pool.size]
    }
}

data class HouseholdMember(
    val uid: String,
    val name: String,
    val emoji: String = "👤",
    val isOwner: Boolean = false,
    val joinedAt: Long = 0L,
)

data class Household(
    val id: String,
    val name: String,
    val inviteCode: String,
    val ownerId: String,
)
