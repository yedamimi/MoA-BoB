package com.eatda.app.util

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.eatda.app.data.model.ScanResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object CameraLabelerService {
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder().setConfidenceThreshold(0.60f).build()
    )

    suspend fun detectFood(bitmap: Bitmap): List<ScanResult> =
        suspendCancellableCoroutine { cont ->
            labeler.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { labels ->
                    val results = labels
                        .mapNotNull { label ->
                            val korean = englishToKorean[label.text.lowercase()] ?: return@mapNotNull null
                            ScanResult(
                                id = "${System.currentTimeMillis()}_${label.text}",
                                name = korean,
                                confidence = label.confidence.toDouble(),
                                isAllergen = false,
                                qty = "1개",
                            )
                        }
                        .take(3)
                    cont.resume(results)
                }
                .addOnFailureListener { cont.resume(emptyList()) }
        }

    private val englishToKorean = mapOf(
        "apple" to "사과",
        "banana" to "바나나",
        "orange" to "오렌지",
        "strawberry" to "딸기",
        "grape" to "포도",
        "watermelon" to "수박",
        "pineapple" to "파인애플",
        "mango" to "망고",
        "peach" to "복숭아",
        "lemon" to "레몬",
        "pear" to "배",
        "cherry" to "체리",
        "blueberry" to "블루베리",
        "kiwi" to "키위",
        "melon" to "멜론",
        "fig" to "무화과",
        "carrot" to "당근",
        "broccoli" to "브로콜리",
        "tomato" to "토마토",
        "potato" to "감자",
        "sweet potato" to "고구마",
        "onion" to "양파",
        "garlic" to "마늘",
        "cucumber" to "오이",
        "lettuce" to "상추",
        "spinach" to "시금치",
        "cabbage" to "양배추",
        "pepper" to "고추",
        "bell pepper" to "파프리카",
        "mushroom" to "버섯",
        "corn" to "옥수수",
        "pumpkin" to "호박",
        "eggplant" to "가지",
        "celery" to "셀러리",
        "zucchini" to "애호박",
        "radish" to "무",
        "green onion" to "파",
        "bean sprout" to "콩나물",
        "beef" to "소고기",
        "pork" to "돼지고기",
        "chicken" to "닭고기",
        "duck" to "오리고기",
        "fish" to "생선",
        "salmon" to "연어",
        "tuna" to "참치",
        "shrimp" to "새우",
        "squid" to "오징어",
        "crab" to "게",
        "clam" to "조개",
        "oyster" to "굴",
        "octopus" to "문어",
        "egg" to "계란",
        "milk" to "우유",
        "cheese" to "치즈",
        "butter" to "버터",
        "yogurt" to "요거트",
        "cream" to "크림",
        "bread" to "빵",
        "rice" to "쌀",
        "noodle" to "면",
        "tofu" to "두부",
        "kimchi" to "김치",
    )
}
