package com.example.eeum.voice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.eeum.R
import com.example.eeum.data.remote.DeviceApi

class VoiceService : Service() {

    private var tts: TtsHelper? = null
    private var pv: PicovoiceManagerEngine? = null
    private lateinit var router: IntentRouter
    val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)

    override fun onCreate() {
        super.onCreate()
        startAsForeground()

        // 준비물
        tts = TtsHelper(this)

        val deviceApi = object : DeviceApi {
            override fun turnOn(device: String, room: String) { /* TODO */ }
            override fun turnOff(device: String, room: String) { /* TODO */ }
            override fun setLevel(device: String, room: String, level: Int) { /* TODO */ }
        }

//        val chat: ChatClient = HttpChatClient(
//            url = BuildConfig.LLM_ENDPOINT,   // ★ API 키/엔드포인트는 BuildConfig 등으로 주입
//            apiKey = BuildConfig.LLM_API_KEY
//        )
//
//        val sttFactory = {
//            SttHelper(
//                ctx = this,
//                onResult = { text ->
//                    // Q/A 모드에서 답변 만들고 TTS
//                    // (원한다면 로딩 사운드/토스트 추가)
//                    // 코루틴 사용을 권하면, 여기선 간단히 서비스 스코프를 만들어도 됨
//                    Thread {
//                        try {
//                            val answer = chat.ask(text)
//                            tts?.say(answer)
//                        } catch (e: Exception) {
//                            tts?.say("죄송해요. 통신 오류가 발생했어요.")
//                        }
//                    }.start()
//                },
//                onError = { err ->
//                    tts?.say("음성을 알아듣지 못했어요.")
//                }
//            )
//        }
//
//        router = IntentRouter(deviceApi, tts!!, chat, sttFactory)

        // Picovoice(핫워드+Rhino NLU) 시작
        pv = PicovoiceManagerEngine(
            context = this,
            accessKey = ai.metaData.getString("PICOVOICE_ACCESS_KEY") ?: "",
            wakeRes   = R.raw.jenny,  // jarvis.ppn (raw 리소스)
            rhinoRes  = R.raw.eeum, // home_ko.rhn (raw 리소스)
            onInference = { intentResult ->
                router.handle(intentResult)
            }
        ).also { it.start() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        pv?.stop(); pv = null
        tts?.shutdown(); tts = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val channelId = "voice_hotword"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "항상 듣기", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notif = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("항상 듣는 중")
            .setContentText("핫워드를 기다리고 있어요")
            .setOngoing(true)
            .build()
        startForeground(1001, notif)
    }
}