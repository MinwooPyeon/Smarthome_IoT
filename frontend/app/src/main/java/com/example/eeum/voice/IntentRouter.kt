package com.example.eeum.voice

import com.example.eeum.data.remote.DeviceApi

class IntentRouter(
    private val deviceApi: DeviceApi,
    private val tts: TtsHelper,             // ← TTS → TtsHelper
    private val chat: ChatClient,
    private val sttFactory: () -> SttHelper // 콜백 이미 세팅된 인스턴스 반환하도록
) {
    fun handle(r: IntentResult) = when (r.name) {
        "TURN_ON_DEVICE", "TURN_OFF_DEVICE", "SET_LEVEL_DEVICE", "SHOW_ENERGY_DASHBOARD" -> handleControl(r)
        "OPEN_QA", "HELP" -> runChat()
        else -> runChat()
    }

    private fun handleControl(r: IntentResult) {
        val s = r.slots
        when (r.name) {
            "TURN_ON_DEVICE" ->
                deviceApi.turnOn(s["device"] ?: "light", s["room"] ?: "living").also { tts.say("켰습니다.") }
            "TURN_OFF_DEVICE" ->
                deviceApi.turnOff(s["device"] ?: "light", s["room"] ?: "living").also { tts.say("꺼드렸어요.") }
            "SET_LEVEL_DEVICE" ->
                deviceApi.setLevel(
                    s["device"] ?: "light",
                    s["room"] ?: "living",
                    s["level"]?.toIntOrNull() ?: 50
                ).also { tts.say("설정 완료.") }
            "SHOW_ENERGY_DASHBOARD" -> tts.say("에너지 사용량을 표시합니다.")
        }
    }

    private fun runChat() {
        val stt = sttFactory()
        stt.start()
        // onResult/onError는 sttFactory에서 넘긴 람다로 처리됨
    }
}