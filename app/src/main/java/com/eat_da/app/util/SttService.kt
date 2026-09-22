package com.eatda.app.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android SpeechRecognizer 래퍼 — STT StateFlow 제공
 * 반드시 Main 스레드에서 start()/stop() 호출
 */
object SttService {

    sealed class SttState {
        object Idle      : SttState()
        object Listening : SttState()
        data class Result(val text: String, val candidates: List<String> = emptyList()) : SttState()
        data class Error(val code: Int)     : SttState()
    }

    private val _state = MutableStateFlow<SttState>(SttState.Idle)
    val state: StateFlow<SttState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    /** 마이크 청취 시작 — Main 스레드에서 호출 */
    fun start(context: Context) {
        stop()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = SttState.Error(-1)
            return
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?) { _state.value = SttState.Listening }
                override fun onBeginningOfSpeech()        {}
                override fun onRmsChanged(r: Float)       {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech()              {}
                override fun onResults(results: Bundle?) {
                    val candidates = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?: emptyList()
                    _state.value = SttState.Result(
                        text       = candidates.firstOrNull().orEmpty(),
                        candidates = candidates,
                    )
                }
                override fun onPartialResults(p: Bundle?) {}
                override fun onEvent(t: Int, p: Bundle?)  {}
                override fun onError(error: Int)          { _state.value = SttState.Error(error) }
            })
            startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                }
            )
        }
    }

    /** 청취 중단 */
    fun stop() {
        recognizer?.runCatching { stopListening(); destroy() }
        recognizer = null
    }

    /** 상태만 IDLE로 리셋 (recognizer는 유지) */
    fun reset() { _state.value = SttState.Idle }
}
