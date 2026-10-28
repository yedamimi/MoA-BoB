package com.eatda.app.data.model

import java.time.LocalDate

enum class FoodCategory(val label: String, val hexColor: Long) {
    VEGETABLE("채소", 0xFF3F8B4F),
    FRUIT("과일", 0xFFE8654F),
    MEAT("육류", 0xFFB23A48),
    SEAFOOD("해산물", 0xFF3B7CB7),
    DAIRY("유제품", 0xFFD9A55A),
    GRAIN("곡물", 0xFFA88560),
    BEVERAGE("음료", 0xFF7A6BA8),
    SAUCE("소스", 0xFF9B8043),
    PROCESSED("가공식품", 0xFF8A6F5A),
}



data class FoodItem(
    val id:  String,
    val name: String,
    val category: FoodCategory,
    val expiry: LocalDate,
    val qty: String,
    val freshness: Int? = null,
    val freshnessClass: String? = null,
    val freshnessConf: Double? = null,
    val location: String,
    val addedDays: Int,
    val isAllergen: Boolean = false,
    val hasRemnant: Boolean = false,
) {
    fun daysLeft(today: LocalDate = LocalDate.now()): Int =
        (expiry.toEpochDay() - today.toEpochDay()).toInt()

    fun expiryStatus(today: LocalDate = LocalDate.now()): ExpiryStatus {
        val d = daysLeft(today)
        return when {
            d < 0 -> ExpiryStatus.EXPIRED
            d <= 1 -> ExpiryStatus.CRITICAL
            d <= 3 -> ExpiryStatus.WARNING
            d <= 7 -> ExpiryStatus.SOON
            else -> ExpiryStatus.FRESH
        }
    }

    fun formatExpiry(today: LocalDate = LocalDate.now()): String {
        val d = daysLeft(today)
        return when {
            d < 0 -> "${-d}일 경과"
            d == 0 -> "오늘까지"
            d == 1 -> "내일까지"
            else -> "D-$d"
        }
    }

    // 신선도 미분석(null 또는 scan.py 기본값 -1)이면 null
    fun freshnessStatus(): FreshnessStatus? = freshnessStatusOf(freshness)
}

enum class ExpiryStatus { EXPIRED, CRITICAL, WARNING, SOON, FRESH }

// 신선도 등급 (앱 전체 공통 기준)
// 70% 이상 신선 / 30% 초과 ~ 70% 미만 보통 / 30% 이하 부패
enum class FreshnessStatus(val label: String) {
    ROTTEN("부패"),
    ATTENTION("보통"),
    FRESH("신선"),
}

fun freshnessStatusOf(score: Int?): FreshnessStatus? = when {
    score == null || score < 0 -> null
    score >= 70 -> FreshnessStatus.FRESH
    score > 30 -> FreshnessStatus.ATTENTION
    else -> FreshnessStatus.ROTTEN
}

// 식재료 이름별 최근 신선도 확인 기록 (Firebase MoA-BoB/freshnessChecks/{이름})
// 같은 이름의 재고 묶음은 겉으로 구분할 수 없으므로 묶음이 아니라 이름 단위로 기록
data class FreshnessCheck(
    val score: Int,
    val checkedAt: Long,   // epoch millis
) {
    val status: FreshnessStatus get() = freshnessStatusOf(score) ?: FreshnessStatus.ROTTEN

    // "방금 전", "15분 전", "3시간 전", "2일 전"
    fun agoText(now: Long = System.currentTimeMillis()): String {
        val minutes = (now - checkedAt).coerceAtLeast(0) / 60_000
        return when {
            minutes < 1 -> "방금 전"
            minutes < 60 -> "${minutes}분 전"
            minutes < 60 * 24 -> "${minutes / 60}시간 전"
            else -> "${minutes / (60 * 24)}일 전"
        }
    }

    // "10월 28일 14:30"
    fun checkedAtText(): String =
        java.time.Instant.ofEpochMilli(checkedAt)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("M월 d일 HH:mm"))

    // 확인한 지 오래되면 다시 확인 권장
    fun isStale(now: Long = System.currentTimeMillis()): Boolean =
        now - checkedAt > FRESHNESS_STALE_DAYS * 24 * 60 * 60 * 1000L
}

const val FRESHNESS_STALE_DAYS = 3

data class Recipe(
    val id: String,
    val title: String,
    val tag: String,
    val minutes: Int,
    val ingredients: List<String>,
    val priority: String? = null,
    val description: String = "",

    // 영양정보
    val calories: Double = 0.0,
    val carbohydrates: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val sodium: Double = 0.0,

    // 조리과정
    val cookingSteps: List<String> = emptyList(),

    // 음식 사진(식품안전나라)
    val imageUrl: String = "",
)

data class AppNotification(
    val id: String,
    val type: NotifType,
    val title: String,
    val body: String,
    val time: String,
    val urgent: Boolean = false,
)

enum class NotifType { EXPIRY_SOON, ROT, TIP }

data class Allergen(
    val id: String,
    val name: String,
    val addedAt: String,
)

data class ScanResult(
    val id: String,
    val name: String,
    val confidence: Double? = null,
    val suggestedExpiry: LocalDate? = null,
    val category: FoodCategory? = null,
    val freshness: Int? = null,
    val rotPct: Int? = null,
    val freshnessStatus: FreshnessStatus? = null,
    val isAllergen: Boolean = false,
    val qty: String = "",
    val itemId: String? = null,   // 입고 결과: scan.py가 저장한 foodInventory 키
)

enum class ScanMode { IN, OUT, FRESH }
