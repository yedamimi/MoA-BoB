package com.eatda.app.util

import android.util.Log
import com.eatda.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

object VisionService {

    private const val ENDPOINT =
        "https://vision.googleapis.com/v1/images:annotate"

    suspend fun extractExpiryDate(
        imageBytes: ByteArray
    ): LocalDate? = withContext(Dispatchers.IO) {

        val key = BuildConfig.EATDA_VISION_KEY

        if (key.isBlank()) {
            Log.e("VISION", "API KEY EMPTY")
            return@withContext null
        }

        return@withContext try {

            val base64 = android.util.Base64.encodeToString(
                imageBytes,
                android.util.Base64.NO_WRAP
            )

            val body = JSONObject().apply {

                put(
                    "requests",
                    JSONArray().put(
                        JSONObject().apply {

                            put(
                                "image",
                                JSONObject().put(
                                    "content",
                                    base64
                                )
                            )

                            put(
                                "features",
                                JSONArray().put(
                                    JSONObject().put(
                                        "type",
                                        "DOCUMENT_TEXT_DETECTION"
                                    )
                                )
                            )

                            put(
                                "imageContext",
                                JSONObject().put(
                                    "languageHints",
                                    JSONArray().apply {
                                        put("ko")
                                        put("en")
                                    }
                                )
                            )
                        }
                    )
                )

            }.toString().toByteArray()

            val conn =
                URL("$ENDPOINT?key=$key")
                    .openConnection() as HttpURLConnection

            conn.requestMethod = "POST"

            conn.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            conn.doOutput = true

            conn.outputStream.use {
                it.write(body)
            }

            val responseCode = conn.responseCode

            Log.d(
                "VISION",
                "responseCode = $responseCode"
            )

            if (responseCode != 200) {

                Log.e(
                    "VISION",
                    "Vision API Error: $responseCode"
                )

                return@withContext null
            }

            val responseText =
                conn.inputStream
                    .bufferedReader()
                    .readText()

            Log.d(
                "VISION_RESPONSE",
                responseText
            )

            val fullText = JSONObject(responseText)
                .getJSONArray("responses")
                .getJSONObject(0)
                .optJSONObject("fullTextAnnotation")
                ?.getString("text")
                ?: return@withContext null

            Log.d(
                "OCR_RESULT_RAW",
                fullText
            )

            // OCR 결과 정규화
            val normalizedText = fullText
                .replace("\n", " ")
                .replace(" ", "")
                .replace("O", "0")
                .replace("o", "0")
                .replace("I", "1")
                .replace("l", "1")
                .replace("Z", "2")
                .replace(",", ".")
                .replace(":", ".")

            Log.d(
                "OCR_RESULT_NORMALIZED",
                normalizedText
            )

            parseExpiryDate(normalizedText)

        } catch (e: Exception) {

            Log.e(
                "VISION_ERROR",
                e.stackTraceToString()
            )

            null
        }
    }

    private fun parseExpiryDate(
        text: String
    ): LocalDate? {

        Log.d("PARSE_TEXT", text)

        val patterns = listOf(

            // 사용기한 : 2027.10.15
            Regex( """(?:유통기한|소비기한|사용기한|까지|EXP|BEST|BBE)[^0-9]*(\d{4})[.\-/]?(\d{1,2})[.\-/]?(\d{1,2})""", RegexOption.IGNORE_CASE ),

            // 2027.10.15
            Regex(
                """\b(\d{4})[.\-/](0?[1-9]|1[0-2])[.\-/](0?[1-9]|[12]\d|3[01])\b"""
            ),

            // 27.10.15
            Regex(
                """\b(\d{2})[.\-/](0?[1-9]|1[0-2])[.\-/](0?[1-9]|[12]\d|3[01])\b"""
            ),

            // 20271015
            Regex(
                """\b(\d{4})(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\b"""
            ),

            // 271015
            Regex(
                """\b(\d{2})(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\b"""
            )
        )

        for (pattern in patterns) {

            val match = pattern.find(text)

            if (match != null) {

                Log.d(
                    "MATCHED_PATTERN",
                    match.value
                )

                return runCatching {

                    val (y, mo, d) =
                        match.destructured

                    val year =
                        if (y.length == 2) {
                            2000 + y.toInt()
                        } else {
                            y.toInt()
                        }

                    val resultDate = LocalDate.of(
                        year,
                        mo.toInt(),
                        d.toInt()
                    )

                    Log.d(
                        "PARSED_DATE",
                        resultDate.toString()
                    )

                    resultDate

                }.getOrElse {

                    Log.e(
                        "DATE_PARSE_ERROR",
                        it.stackTraceToString()
                    )

                    null
                }
            }
        }

        Log.e(
            "DATE_PARSE",
            "NO DATE FOUND"
        )

        return null
    }

}