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
import com.example.eeum.core.AppEffect
import com.example.eeum.core.AppEventBus
import com.example.eeum.core.VoiceUseCase
import com.example.eeum.data.model.dto.voice.IntentResult
import com.example.eeum.data.model.dto.voice.NluUpdate
import com.example.eeum.data.remote.RetrofitUtil
import com.example.eeum.data.remote.repository.DeviceRepository
import com.example.eeum.data.remote.repository.RoutineRepository
import com.example.eeum.util.ResourceUtils
import com.example.eeum.util.RuleCompiler
import com.example.eeum.util.VoiceBus
import com.example.eeum.util.VoiceDeps
import kotlinx.coroutines.launch

private const val TAG = "EEUM_VoiceService"
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

    private lateinit var useCase: VoiceUseCase
    @Volatile private var isListening = false

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        initEarcon()
        tts = TtsHelper(this)

        val grammar = ResourceUtils.loadFromAssets(this, "grammar.yml")
        val compiled = RuleCompiler.compile(grammar)
        val connectors = grammar.context.macros["연결하다"] ?: listOf("그리고","하고","그 다음에","다음에","한 다음에","이어서","켜고","끄고")
        nlu = NluEngine(compiled, connectors)

        VoiceDeps.directory?.let { dir ->
            val repo = DeviceRepository(RetrofitUtil.deviceService, dir)
            useCase = VoiceUseCase(repo)
            Log.d(TAG, "UseCase ready with directory cache")
        } ?: run {
            Log.d(TAG, "Directory cache not ready yet; will lazy-init on first command")
        }

//        Handler(Looper.getMainLooper()).postDelayed({
//            tts?.say("음성 명령을 말씀해 주세요.")
//        }, 600)
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
            Log.e(TAG, "SpeechRecognizer not available"); return
        }
        if (isListening) { Log.d(TAG, "already listening"); return }
        isListening = true

        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    val text = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )?.firstOrNull().orEmpty()
                    Log.i(TAG, "ASR: $text")
                    playEarcon()
                    handleUtterance(text)
                    shutdownRecognizer()
                }
                override fun onError(error: Int) {
                    Log.e(TAG, "ASR error: $error")
                    shutdownRecognizer()
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

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer?.startListening(intent)
    }

    private fun stopListening() { shutdownRecognizer() }

    private fun handleUtterance(text: String) {
        val intents = nlu.parseUtterance(text)
        if (intents.isEmpty()) {
            tts?.say("이해하지 못했어요.")
            ProcessLifecycleOwner.get().lifecycleScope.launch {
                VoiceBus.updates.emit(NluUpdate(text, emptyList()))
            }
            return
        }

        if (!::useCase.isInitialized) {
            val dir = VoiceDeps.directory
            if (dir == null) {
                tts?.say("아직 디바이스 준비 중이에요.")
                Log.d(TAG, "UseCase init skipped: directory cache is null")
                return
            } else {
                val repo = DeviceRepository(RetrofitUtil.deviceService, dir)
                val routineRepo = RoutineRepository(RetrofitUtil.routineService)
                useCase = VoiceUseCase(repo, routineRepo)
                Log.d(TAG, "UseCase lazily initialized")
            }
        }

        ProcessLifecycleOwner.get().lifecycleScope.launch {
            try {
                val effects = useCase.run(intents, text)
                effects.forEach { eff ->
                    when (eff) {
                        is AppEffect.Speak -> {
                            Log.i(TAG, "Speak: ${eff.text}")
                            tts?.say(eff.text)
                        }
                        is AppEffect.Navigate -> {
                            Log.i(TAG, "Navigate(route=${eff.route}, params=${eff.params})")
                            // 나중에 실제 네비게이션 붙일 것. (현재는 로그만)
                        }
                        is AppEffect.Toast -> {
                            Log.i(TAG, "Toast: ${eff.text}")
                            // 나중에 토스트/알림 처리
                        }
                    }
                    // 앞으로 UI가 붙었을 때도 동일 효과를 재사용할 수 있도록 브로드캐스트
                    AppEventBus.tryEmit(eff)
                }
            } catch (e: Exception) {
                Log.e(TAG, "UseCase run failed", e)
                tts?.say("처리에 실패했어요.")
            }
        }

        // (선택) UI로 NLU 결과 흘리기 – 기존 유지
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            VoiceBus.updates.emit(
                NluUpdate(
                    raw = text,
                    intents = intents.map { IntentResult(it.intent, it.slots) }
                )
            )
        }
    }

    private fun shutdownRecognizer() {
        val r = recognizer
        recognizer = null
        try { r?.setRecognitionListener(null) } catch (_: Throwable) {}
        try { r?.destroy() } catch (_: Throwable) {}
        isListening = false
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
