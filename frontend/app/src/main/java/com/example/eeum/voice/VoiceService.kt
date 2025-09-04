package com.example.eeum.voice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.eeum.R
import com.example.eeum.data.remote.DeviceApi

class VoiceService : Service() {

    private var tts: TtsHelper? = null
    private var pv: PicovoiceManagerEngine? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()

        tts = TtsHelper(this)

        // meta-data에서 키 읽기 (onCreate 안에서)
        val appInfo = packageManager.getApplicationInfo(
            packageName,
            PackageManager.GET_META_DATA
        )
        val pvKey = appInfo.metaData?.getString("PICOVOICE_ACCESS_KEY").orEmpty()

        // Picovoice 시작: 핫워드 상시 듣기 + 핫워드 이후 Rhino NLU
        pv = PicovoiceManagerEngine(
            context = this,
            accessKey = pvKey,
            wakeRes   = R.raw.jenny,
            rhinoRes  = R.raw.eeum,
            wakeResModel   = R.raw.porcupine_params_ko,
            rhinoResModel  = R.raw.rhino_params_ko,
            onInference = { r ->
                // 여기서 "핫워드 후" 발화가 파싱됨
                // 오늘은 API 없이 결과만 확인
                Log.i("VoiceService", "Intent=${r.name}, slots=${r.slots}")
                tts?.say("인텐트 ${r.name} 인식했어요.")
            }
        ).also { it.start() }

        Handler(Looper.getMainLooper()).postDelayed({
            tts?.say("호출어를 말해 주세요.")
        }, 800)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

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
