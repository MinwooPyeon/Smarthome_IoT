package com.example.eeum.voice

import com.example.eeum.data.model.dto.voice.IntentResult

class IntentRouter(

    private val tts: TtsHelper,             // ← TTS → TtsHelper
    private val chat: ChatClient,
    private val sttFactory: () -> SttHelper // 콜백 이미 세팅된 인스턴스 반환하도록
) {
    fun handle(r: IntentResult) = when (r.intent) {
        "TURN_ON_DEVICE", "TURN_OFF_DEVICE", "SET_LEVEL_DEVICE", "SHOW_ENERGY_DASHBOARD" -> handleControl(r)
        "OPEN_QA", "HELP" -> runChat()
        else -> runChat()
    }

    private fun handleControl(r: IntentResult) {
        val s = r.slots
        when (r.intent) {

        }
    }

    private fun runChat() {
        val stt = sttFactory()
        stt.start()
        // onResult/onError는 sttFactory에서 넘긴 람다로 처리됨
    }
}