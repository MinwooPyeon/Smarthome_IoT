package com.example.eeum.voice

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale


class TtsHelper(ctx: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(ctx, this)
    private var ready = false
    private var onDone: (() -> Unit)? = null

    override fun onInit(status: Int) {
        ready = (status == TextToSpeech.SUCCESS)
        if (ready) {
            tts?.language = Locale.KOREAN
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { /* no-op */ }

                override fun onDone(utteranceId: String?) {
                    // 메인 스레드에서 after 콜백 호출 (살짝 텀)
                    val cb = onDone
                    onDone = null
                    if (cb != null) {
                        Handler(Looper.getMainLooper()).postDelayed(cb, 150L)
                    }
                }

                override fun onError(utteranceId: String?) {
                    onDone = null
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    onDone = null
                }
            })
        }
    }

    fun say(text: String, after: (() -> Unit)? = null) {
        if (!ready) return
        onDone = after
        val params = Bundle()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "utt-${System.nanoTime()}")
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}