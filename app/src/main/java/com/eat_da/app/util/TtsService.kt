package com.eatda.app.util

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.eatda.app.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

// 음성 안내. 들어온 문장을 순서대로 하나씩 끝까지 재생한다.
// (재생 중에 새 문장이 와도 앞 문장을 끊지 않고 이어서 읽음)
object TtsService {
    private const val ENDPOINT = "https://texttospeech.googleapis.com/v1/text:synthesize"

    // 대기 중이거나 재생 중인 문장이 있으면 true
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val lock = Any()
    private val queue = ArrayDeque<String>()
    private var appContext: Context? = null
    private var worker: Job? = null

    private var fallbackTts: TextToSpeech? = null

    // 문장을 대기열 끝에 추가. 바로 반환하며, 재생은 순서대로 진행됨
    fun speak(context: Context, text: String) {
        if (text.isBlank()) return
        synchronized(lock) {
            appContext = context.applicationContext
            queue.addLast(text)
            if (worker == null) worker = scope.launch { drain() }
        }
    }

    // 재생 중인 문장과 대기 중인 문장을 모두 취소
    fun stop() {
        synchronized(lock) {
            queue.clear()
            worker?.cancel()
            worker = null
        }
        fallbackTts?.stop()
        _isPlaying.value = false
    }

    private suspend fun drain() {
        _isPlaying.value = true
        try {
            while (true) {
                val text = synchronized(lock) {
                    queue.removeFirstOrNull() ?: run { worker = null; null }
                } ?: break
                val context = appContext ?: break
                try {
                    play(context, text)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // 한 문장 재생에 실패해도 다음 문장은 계속 재생
                }
            }
        } finally {
            _isPlaying.value = false
        }
    }

    private suspend fun play(context: Context, text: String) {
        val key = BuildConfig.EATDA_TTS_KEY
        val audio = if (key.isBlank()) null else synthesize(key, text)
        if (audio == null) playFallback(context, text) else playMp3(context, audio)
    }

    // Google Cloud TTS로 MP3 생성. 실패하면 null → 기본 TTS로 대체
    private suspend fun synthesize(key: String, text: String): ByteArray? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val body = JSONObject().apply {
                put("input", JSONObject().put("text", text))
                put("voice", JSONObject().apply {
                    put("languageCode", "ko-KR")
                    put("name", "ko-KR-Wavenet-A")
                })
                put("audioConfig", JSONObject().put("audioEncoding", "MP3"))
            }.toString().toByteArray()

            conn = (URL("$ENDPOINT?key=$key").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 5_000
                readTimeout    = 10_000
                doOutput = true
            }
            conn.outputStream.use { it.write(body) }

            if (conn.responseCode != 200) return@withContext null

            android.util.Base64.decode(
                JSONObject(conn.inputStream.bufferedReader().readText()).getString("audioContent"),
                android.util.Base64.DEFAULT,
            )
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    // MP3를 재생하고 끝날 때까지 기다림
    private suspend fun playMp3(context: Context, audio: ByteArray) {
        val file = File(context.cacheDir, "tts_${System.nanoTime()}.mp3")
        val player = MediaPlayer()
        try {
            withContext(Dispatchers.IO) {
                file.writeBytes(audio)
                player.setDataSource(file.absolutePath)
                player.prepare()
            }
            suspendCancellableCoroutine<Unit> { cont ->
                player.setOnCompletionListener { if (cont.isActive) cont.resume(Unit) }
                player.setOnErrorListener { _, _, _ ->
                    if (cont.isActive) cont.resume(Unit)
                    true
                }
                // stop() 버튼 → 즉시 소리 끄기
                cont.invokeOnCancellation { runCatching { player.stop() } }
                player.start()
            }
        } finally {
            runCatching { player.release() }
            file.delete()
        }
    }

    // Android 기본 TTS 폴백 (네트워크 없거나 키가 없을 때). 끝날 때까지 기다림
    private suspend fun playFallback(context: Context, text: String) {
        val tts = fallbackEngine(context) ?: return
        val utteranceId = "tts_${System.nanoTime()}"

        suspendCancellableCoroutine<Unit> { cont ->
            fun finish(id: String?) {
                if (id == utteranceId && cont.isActive) cont.resume(Unit)
            }
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) = finish(id)
                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) = finish(id)
                override fun onStop(id: String?, interrupted: Boolean) = finish(id)
            })
            cont.invokeOnCancellation { tts.stop() }

            if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId) == TextToSpeech.ERROR) {
                finish(utteranceId)
            }
        }
    }

    // 기본 TTS 엔진은 처음 한 번만 초기화
    private suspend fun fallbackEngine(context: Context): TextToSpeech? {
        fallbackTts?.let { return it }
        return suspendCancellableCoroutine<TextToSpeech?> { cont ->
            var engine: TextToSpeech? = null
            engine = TextToSpeech(context.applicationContext) { status ->
                val ready = engine
                if (status == TextToSpeech.SUCCESS && ready != null) {
                    ready.language = Locale.KOREAN
                    fallbackTts = ready
                    if (cont.isActive) cont.resume(ready)
                } else {
                    ready?.shutdown()
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
    }
}
