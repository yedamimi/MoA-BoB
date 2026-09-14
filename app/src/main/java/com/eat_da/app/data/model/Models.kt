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
}

data class FoodItem(
    val id: Int,
    val name: String,
    val category: FoodCategory,
    val expiry: LocalDate,
    val qty: String,
    val freshness: Int? = null,
    val location: String,
    val addedDays: Int,
    val isAllergen: Boolean = false,
) {
    fun daysLeft(today: LocalDate = LocalDate.of(2026, 5, 6)): Int =
        (expiry.toEpochDay() - today.toEpochDay()).toInt()

    fun expiryStatus(today: LocalDate = LocalDate.of(2026, 5, 6)): ExpiryStatus {
        val d = daysLeft(today)
        return when {
            d < 0 -> ExpiryStatus.EXPIRED
            d <= 1 -> ExpiryStatus.CRITICAL
            d <= 3 -> ExpiryStatus.WARNING
            d <= 7 -> ExpiryStatus.SOON
            else -> ExpiryStatus.FRESH
        }
    }

    fun formatExpiry(today: LocalDate = LocalDate.of(2026, 5, 6)): String {
        val d = daysLeft(today)
        return when {
            d < 0 -> "${-d}일 경과"
            d == 0 -> "오늘까지"
            d == 1 -> "내일까지"
            else -> "D-$d"
        }
    }

    fun freshnessStatus(): FreshnessStatus? = freshness?.let {
        when {
            it < 60 -> FreshnessStatus.ROTTEN
            it < 80 -> FreshnessStatus.ATTENTION
            else -> FreshnessStatus.FRESH
        }
    }
}

enum class ExpiryStatus { EXPIRED, CRITICAL, WARNING, SOON, FRESH }
enum class FreshnessStatus { ROTTEN, ATTENTION, FRESH }

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
)

enum class ScanMode { IN, OUT, FRESH, ALLERGEN }