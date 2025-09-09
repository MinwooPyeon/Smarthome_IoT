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
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.eeum.R
import com.example.eeum.data.model.dto.voice.IntentResult
import com.example.eeum.data.model.dto.voice.NluUpdate
import com.example.eeum.util.ResourceUtils
import com.example.eeum.util.RuleCompiler
import com.example.eeum.util.VoiceBus
import kotlinx.coroutines.launch

class VoiceService : Service() {

    companion object {
        const val ACTION_START_LISTEN = "com.example.eeum.voice.START_LISTEN"
        const val ACTION_STOP_LISTEN  = "com.example.eeum.voice.STOP_LISTEN"
    }

    private var tts: TtsHelper? = null
    private var soundPool: SoundPool? = null
    private var earconId: Int = 0
    private var earconReady = false
    private var lastPlayAt = 0L

    private val CHANNEL_ID = "voice_hotword"
    private val NOTIF_ID = 1001

    // STT
    private var recognizer: SpeechRecognizer? = null

    // NLU
    private lateinit var nlu: NluEngine

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        initEarcon()
        tts = TtsHelper(this)

        // 1) YAML 로드 → 컴파일
        val grammar = ResourceUtils.loadFromAssets(this, "grammar.yml")
        val compiled = RuleCompiler.compile(grammar)
        val connectors = grammar.context.macros["연결하다"] ?: listOf("그리고","하고","그 다음에","다음에","한 다음에","이어서","켜고","끄고")
        nlu = NluEngine(compiled, connectors)

        // 2) 안내
        Handler(Looper.getMainLooper()).postDelayed({
            tts?.say("음성 명령을 말씀해 주세요.")
        }, 600)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LISTEN -> startListening()
            ACTION_STOP_LISTEN  -> stopListening()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        releaseEarcon()
        stopListening()
        tts?.shutdown(); tts = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // STT
    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e("VoiceService", "SpeechRecognizer not available")
            return
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onResults(results: Bundle) {
                        val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                        Log.i("VoiceService", "ASR: $text")
                        playEarcon()
                        handleUtterance(text)
                        stopListening()
                    }
                    override fun onError(error: Int) {
                        Log.e("VoiceService", "ASR error: $error")
                        stopListening()
                    }
                    override fun onReadyForSpeech(p0: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(p0: Float) {}
                    override fun onBufferReceived(p0: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onPartialResults(p0: Bundle?) {}
                    override fun onEvent(p0: Int, p1: Bundle?) {}
                })
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer?.startListening(intent)
    }


    private fun stopListening() {
        recognizer?.stopListening()
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private fun handleUtterance(text: String) {
        val intents = nlu.parseUtterance(text)
        if (intents.isEmpty()) {
            tts?.say("이해하지 못했어요.")
            // UI에도 실패 결과 흘려보내기
            ProcessLifecycleOwner.get().lifecycleScope.launch {
                VoiceBus.updates.emit(NluUpdate(text, emptyList()))
            }
            return
        }

        // 라우팅: 예시로 로그 + TTS
        intents.forEach { r ->
            Log.i("VoiceService", "Intent=${r.intent}, slots=${r.slots}")
        }

        val summary = intents.joinToString(" 그리고 ") { r ->
            val target = makeRoomKey(r.slots)?.let { "[$it]" } ?: ""
            "${r.intent}${if (target.isNotEmpty()) " $target" else ""}"
        }
        tts?.say("$summary 실행할게요.")

        // UI로 결과 emit
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            VoiceBus.updates.emit(
                NluUpdate(
                    raw = text,
                    intents = intents.map { IntentResult(it.intent, it.slots) }
                )
            )
        }

        // TODO: 실제 디바이스 API 호출로 연결
    }

    private fun makeRoomKey(slots: Map<String,String>): String? {
        val name = slots["이름"] ?: return null
        val kin  = slots["호칭"] ?: return null
        val place= slots["장소"] ?: return null
        return "${name}_${kin}_${place}"
    }

    // ────────────────────────────
    // FG 알림 & 이어콘
    // ────────────────────────────
    private fun startAsForeground() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "항상 듣기", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("듣고 있어요 (◕‿◕)")
            .setContentText("말씀해 주세요")
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notif)
    }

    private fun initEarcon() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(1).setAudioAttributes(attrs).build()
        soundPool?.setOnLoadCompleteListener { sp, sampleId, status ->
            if (status == 0 && sampleId == earconId) {
                earconReady = true
                sp.play(earconId, 0f, 0f, 0, 0, 1f) // warm-up
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
        soundPool?.play(earconId, 0.5f, 0.5f, 0, 0, 1f)
    }

    private fun releaseEarcon() {
        soundPool?.release()
        soundPool = null
        earconReady = false
    }
}
