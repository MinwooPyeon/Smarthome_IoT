package com.example.eeum.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale


class TtsHelper(ctx: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(ctx, this)
    private var ready = false

    override fun onInit(status: Int) {
        ready = (status == TextToSpeech.SUCCESS)
        if (ready) tts?.language = Locale.KOREAN
    }

    fun say(text: String) {
        if (ready) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt")
    }

    fun shutdown() { tts?.shutdown(); tts = null }
}