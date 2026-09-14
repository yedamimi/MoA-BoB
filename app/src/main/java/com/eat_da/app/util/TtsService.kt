package com.eatda.app.util

import android.content.Context
import android.media.MediaPlayer
import com.eatda.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object TtsService {
    private const val ENDPOINT = "https://texttospeech.googleapis.com/v1/text:synthesize"

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var currentPlayer: MediaPlayer? = null

    fun stop() {
        currentPlayer?.let { player ->
            runCatching { if (player.isPlaying) player.stop(); player.release() }
        }
        currentPlayer = null
        _isPlaying.value = false
    }

    suspend fun speak(context: Context, text: String) {
        stop()
        val key = BuildConfig.EATDA_TTS_KEY
        if (key.isBlank()) return

        val audioBytes = withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("input", JSONObject().put("text", text))
                put("voice", JSONObject().apply {
                    put("languageCode", "ko-KR")
                    put("name", "ko-KR-Wavenet-A")
                })
                put("audioConfig", JSONObject().put("audioEncoding", "MP3"))
            }.toString().toByteArray()

            val conn = URL("$ENDPOINT?key=$key").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.outputStream.use { it.write(body) }

            if (conn.responseCode != 200) return@withContext null

            android.util.Base64.decode(
                JSONObject(conn.inputStream.bufferedReader().readText()).getString("audioContent"),
                android.util.Base64.DEFAULT,
            )
        } ?: return

        val tempFile = java.io.File(context.cacheDir, "tts.mp3").also { it.writeBytes(audioBytes) }

        val player = MediaPlayer()
        withContext(Dispatchers.IO) {
            player.setDataSource(tempFile.absolutePath)
            player.prepare()
        }
        withContext(Dispatchers.Main) {
            _isPlaying.value = true
            currentPlayer = player
            player.start()
            player.setOnCompletionListener {
                it.release()
                currentPlayer = null
                _isPlaying.value = false
            }
        }
    }
}
 
