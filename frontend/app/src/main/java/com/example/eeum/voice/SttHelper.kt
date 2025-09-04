package com.example.eeum.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class SttHelper(private val ctx: Context,
                private val onResult: (String)->Unit,
                private val onError: (String)->Unit): RecognitionListener {
    private var rec: SpeechRecognizer? = null
    fun start(locale: Locale = Locale.KOREAN) {
        rec = SpeechRecognizer.createSpeechRecognizer(ctx).apply {
            setRecognitionListener(this@SttHelper)
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            })
        }
    }
    fun release() { rec?.destroy(); rec = null }
    override fun onResults(b: Bundle) {
        onResult(b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty())
        release()
    }
    override fun onError(code: Int) { onError("STT:$code"); release() }
    // 나머지 콜백은 no-op
    override fun onReadyForSpeech(p0: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(p0: Float) {}
    override fun onBufferReceived(p0: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(p0: Bundle?) {}
    override fun onEvent(p0: Int, p1: Bundle?) {}
}