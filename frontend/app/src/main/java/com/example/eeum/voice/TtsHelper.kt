package com.example.eeum.voice

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap


class TtsHelper(ctx: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(ctx, this)
    private var ready = false
    private val callbacks = ConcurrentHashMap<String, () -> Unit>()

    override fun onInit(status: Int) {
        ready = (status == TextToSpeech.SUCCESS)
        if (ready) {
            tts?.language = Locale.KOREAN
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { /* no-op */ }

                override fun onDone(utteranceId: String?) {
                    utteranceId?.let { id ->
                        callbacks.remove(id)?.invoke()
                    }
                }

                // 에러 시에도 다음 로직 진행(재청취 등)하도록 onDone과 동일 처리
                override fun onError(utteranceId: String?) {
                    utteranceId?.let { id ->
                        callbacks.remove(id)?.invoke()
                    }
                }
            })
        }
    }

    /**
     * @param flush true면 기존 큐를 비우고 시작(첫 문장), false면 큐에 이어 붙임(후속 문장)
     * @param onDone 해당 문장 재생 완료 콜백
     */
    fun say(text: String, flush: Boolean = true, onDone: (() -> Unit)? = null) {
        if (!ready) return
        val id = UUID.randomUUID().toString()
        if (onDone != null) callbacks[id] = onDone
        val mode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(text, mode, /* params */ null, /* utteranceId */ id)
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        callbacks.clear()
    }
}