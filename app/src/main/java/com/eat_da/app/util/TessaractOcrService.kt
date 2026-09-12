package com.eatda.app.util

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalDate
import kotlin.coroutines.resume

object TesseractOcrService {

    private val recognizer = TextRecognition.getClient(
        KoreanTextRecognizerOptions.Builder().build()
    )

    suspend fun extractExpiryDate(bitmap: Bitmap): LocalDate? =
        suspendCancellableCoroutine { cont ->
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { result -> cont.resume(parseExpiryDate(result.text)) }
                .addOnFailureListener { cont.resume(null) }
        }

    private fun parseExpiryDate(text: String): LocalDate? {
        val patterns = listOf(
            Regex("""(?:유통기한|소비기한|까지|EXP|BEST|BBE)[^0-9]*(\d{4})[.\-/\s](\d{1,2})[.\-/\s](\d{1,2})""", RegexOption.IGNORE_CASE),
            Regex("""(\d{4})[.\-/](0?[1-9]|1[0-2])[.\-/](0?[1-9]|[12]\d|3[01])"""),
            Regex("""(\d{4})(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])"""),
            Regex("""(\d{2})[.\-/](0?[1-9]|1[0-2])[.\-/](0?[1-9]|[12]\d|3[01])"""),
        )
        for (p in patterns) {
            val m = p.find(text) ?: continue
            return runCatching {
                val (y, mo, d) = m.destructured
                val year = if (y.length == 2) 2000 + y.toInt() else y.toInt()
                LocalDate.of(year, mo.toInt(), d.toInt())
            }.getOrNull()
        }
        return null
    }
}
