package com.example.eeum.voice

import android.content.Context
import androidx.annotation.RawRes
import com.example.test.resources.ResourceUtils

class PicovoiceManagerEngine(
    private val context: Context,
    private val accessKey: String,
    @RawRes private val wakeRes: Int,
    @RawRes private val rhinoRes: Int,
    private val onInference: (IntentResult)->Unit
) {
    private var manager: ai.picovoice.picovoice.PicovoiceManager? = null

    fun start() {
        val keywordPath = ResourceUtils.rawToFile(context, wakeRes, "jarvis.ppn")
        val rhinoPath   = ResourceUtils.rawToFile(context, rhinoRes, "home_ko.rhn")

        manager = ai.picovoice.picovoice.PicovoiceManager.Builder()
            .setAccessKey(accessKey)
            .setKeywordPath(keywordPath)
            .setContextPath(rhinoPath)
            .setInferenceCallback { inf ->
                if (inf.isUnderstood) onInference(IntentResult(inf.intent, inf.slots))
            }
            .build(context)
        manager?.start()
    }
    fun stop() { manager?.stop(); manager?.delete(); manager = null }
}