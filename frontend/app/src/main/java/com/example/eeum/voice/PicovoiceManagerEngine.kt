package com.example.eeum.voice

import android.content.Context
import android.util.Log
import androidx.annotation.RawRes
import com.example.eeum.R

// 핫워드+Rhino NLU 시작->콜백 전달.
class PicovoiceManagerEngine(
    private val context: Context,
    private val accessKey: String,
    @RawRes private val wakeRes: Int,
    @RawRes private val rhinoRes: Int,
    @RawRes private val wakeResModel: Int,
    @RawRes private val rhinoResModel: Int,
    private val onInference: (IntentResult)->Unit,
    private val onWake: (() -> Unit)? = null
) {
    private var manager: ai.picovoice.picovoice.PicovoiceManager? = null

    fun start() {
        val keywordPath = ResourceUtils.rawToFile(context, wakeRes, "jenny.ppn")
        val rhinoPath   = ResourceUtils.rawToFile(context, rhinoRes, "eeum.rhn")
        val porcupineModelPath = ResourceUtils.rawToFile(context, wakeResModel,"porcupine_params_ko.pv")
        val rhinoModelPath     = ResourceUtils.rawToFile(context, rhinoResModel,"rhino_params_ko.pv")


        manager = ai.picovoice.picovoice.PicovoiceManager.Builder()
            .setAccessKey(accessKey)
            .setKeywordPath(keywordPath)
            .setContextPath(rhinoPath)
            .setPorcupineModelPath(porcupineModelPath)
            .setRhinoModelPath(rhinoModelPath)
            .setWakeWordCallback {onWake?.invoke()}
            .setInferenceCallback { inf ->
                if (inf.isUnderstood){
                    Log.d("EEUM_PicovoiceManagerEngine", "Rhino OK: intent=${inf.intent}, slots=${inf.slots}")
                    onInference(IntentResult(inf.intent, inf.slots))
                } else {
                    Log.d("EEUM_PicovoiceManagerEngine","Rhino not understood ❌")
                }
            }
            .build(context)
        manager?.start()
    }
    fun stop() { manager?.stop(); manager?.delete(); manager = null }
}