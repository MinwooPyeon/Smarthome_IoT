package com.example.eeum.voice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.eeum.R
import com.example.eeum.data.remote.DeviceApi

class VoiceService : Service() {

    private var tts: TtsHelper? = null
    private var pv: PicovoiceManagerEngine? = null

    private var soundPool: SoundPool? = null
    private var earconId: Int = 0

    @Volatile private var earconReady = false
    private var lastPlayAt = 0L

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        initEarcon()

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
                tts?.say("${r.name} 인식했어요.")
            },
            onWake = { playEarcon() }
        ).also { it.start() }

        Handler(Looper.getMainLooper()).postDelayed({
            tts?.say("호출어를 말해 주세요.")
        }, 800)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        releaseEarcon()
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
            .setContentTitle("듣고 있어요 (◕‿◕)")
            .setContentText("『제니야』라고 불러 주세요")
            .setOngoing(true)
            .build()
        startForeground(1001, notif)
    }

    private fun initEarcon() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(attrs)
            .build()

        soundPool?.setOnLoadCompleteListener { sp, sampleId, status ->
            if (status == 0 && sampleId == earconId) {
                earconReady = true
                // 첫 재생 지연 방지: 무음으로 1회 워밍업
                sp.play(earconId, 0f, 0f, 0, 0, 1f)
            }
        }
        earconId = soundPool!!.load(this, R.raw.earcon_beep, 1)
    }

    private fun playEarcon() {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        if (am.ringerMode == AudioManager.RINGER_MODE_SILENT) return

        if (!earconReady) return

        val now = SystemClock.uptimeMillis()
        if (now - lastPlayAt < 400) return
        lastPlayAt = now

        soundPool?.play(earconId,0.5f,0.5f,0,0,1f)
    }

    private fun releaseEarcon() {
        soundPool?.release()
        soundPool = null
        earconReady = false
    }
}
