package com.example.eeum.voice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.pm.PackageManager
import android.content.Intent
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
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback

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
    @Volatile private var isListening = false

    // NLU
    private lateinit var nlu: NluEngine
    private lateinit var useCase: VoiceUseCase

    // Porcupine (wakeword)
    private var porcupineManager: PorcupineManager? = null
    @Volatile private var isWakewordActive = false
    private var consecutiveMiss = 0
    private lateinit var kwPath: String
    private lateinit var modelPath: String
    private lateinit var pvAccessKey: String

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        initEarcon()
        tts = TtsHelper(this)

        // === NLU 준비 (기존 그대로) ===
        val grammar = ResourceUtils.loadFromAssets(this, "grammar.yml")
        val compiled = RuleCompiler.compile(grammar)
        val connectors = grammar.context.macros["연결하다"] ?: listOf("그리고","하고","그 다음에","다음에","한 다음에","이어서","켜고","끄고")
        nlu = NluEngine(compiled, connectors)

        VoiceDeps.directory?.let { dir ->
            val repo = DeviceRepository(RetrofitUtil.deviceService, dir)
            val routineRepo = RoutineRepository(RetrofitUtil.routineService, VoiceDeps.routineDirectory)
            useCase = VoiceUseCase(repo, routineRepo)
            Log.d(TAG, "UseCase ready with directory cache")
        } ?: run {
            Log.d(TAG, "Directory cache not ready yet; will lazy-init on first command")
        }

        // === Picovoice AccessKey (Manifest meta-data) ===
        pvAccessKey = run {
            val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            ai.metaData.getString("PICOVOICE_ACCESS_KEY") ?: throw IllegalStateException("PICOVOICE_ACCESS_KEY missing")
        }

        // === assets → 내부저장소 경로 확보 ===
        kwPath = copyAssetOnce("jenny_ko_android_v3_0_0.ppn")
        modelPath = copyAssetOnce("porcupine_params_ko.pv")

        // === Porcupine 초기화 & 항상듣기 시작 ===
        initPorcupine()
        startWakeword()
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

        // Porcupine 정리
        safeStopWakeword()
        try { porcupineManager?.delete() } catch (_: Throwable) {}
        porcupineManager = null

        tts?.shutdown(); tts = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ────────────────────────────
    // STT
    // ────────────────────────────
    private fun startListening() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post { startListening() }
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "SpeechRecognizer not available"); return
        }
        if (isListening) { Log.d(TAG, "already listening"); return }
        isListening = true

        // 웨이크워드 → STT 전환: 마이크 단일점유 보장
        safeStopWakeword()

        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    val text = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )?.firstOrNull().orEmpty()
                    Log.i(TAG, "ASR: $text")
                    playEarcon()
                    shutdownRecognizer()
                    handleUtterance(text) // 이 함수에서 성공/실패 분기 + 재개 처리
                }
                override fun onError(error: Int) {
                    Log.e(TAG, "ASR error: $error")
                    shutdownRecognizer()
                    // STT 에러는 "실패"로 간주: 1회째면 재시작, 2회째면 웨이크워드만 재개
                    onUnderstandFail()
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
            onUnderstandFail()
            ProcessLifecycleOwner.get().lifecycleScope.launch {
                VoiceBus.updates.emit(NluUpdate(text, emptyList()))
            }
            return
        }

        // 성공: 실패 카운터 리셋
        consecutiveMiss = 0

        // UseCase 실행
        if (!::useCase.isInitialized) {
            val dir = VoiceDeps.directory
            if (dir == null) {
                tts?.say("아직 디바이스 준비 중이에요.")
                // 웨이크워드로 복귀
                resumeWakewordWithDelay()
                Log.d(TAG, "UseCase init skipped: device directory cache is null")
                return
            } else {
                val repo = DeviceRepository(RetrofitUtil.deviceService, dir)
                val routineRepo = RoutineRepository(RetrofitUtil.routineService, VoiceDeps.routineDirectory)
                useCase = VoiceUseCase(repo, routineRepo)
                Log.d(TAG, "UseCase lazily initialized with routine cache=${VoiceDeps.routineDirectory != null}")
            }
        }

        ProcessLifecycleOwner.get().lifecycleScope.launch {
            try {
                val effects = useCase.run(intents, text)

                val speaks = effects.filterIsInstance<AppEffect.Speak>()
                val hasExpectReply = speaks.any { it.expectReply }

                speaks.forEachIndexed { idx, s ->
                    val isLast = idx == speaks.lastIndex
                    val flush = idx == 0
                    val onDoneCb: (() -> Unit)? =
                        if (isLast && s.expectReply) {
                            { Handler(Looper.getMainLooper()).postDelayed({ startListening() }, 150L) }
                        } else null
                    tts?.say(s.text, flush, onDoneCb)
                }

                effects.forEach { eff ->
                    when (eff) {
                        is AppEffect.Navigate -> Log.i(TAG, "Navigate(route=${eff.route}, params=${eff.params})")
                        is AppEffect.Toast -> Log.i(TAG, "Toast: ${eff.text}")
                        is AppEffect.Speak -> {}
                        is AppEffect.DevicesChanged  -> { }
                        is AppEffect.RoutinesChanged -> { }
                    }
                    AppEventBus.tryEmit(eff)
                }

                // 대화 모드가 아니면 웨이크워드로 복귀
                if (!hasExpectReply) resumeWakewordWithDelay()
            } catch (e: Exception) {
                Log.e(TAG, "UseCase run failed", e)
                tts?.say("처리에 실패했어요.")
                resumeWakewordWithDelay()
            }
        }

        ProcessLifecycleOwner.get().lifecycleScope.launch {
            VoiceBus.updates.emit(
                NluUpdate(
                    raw = text,
                    intents = intents.map { IntentResult(it.intent, it.slots) }
                )
            )
        }
    }

    private fun onUnderstandFail() {
        consecutiveMiss += 1
        val again = consecutiveMiss == 1
        tts?.say("이해하지 못했어요.") {
            if (again) {
                // 1회차 실패: 바로 다시 STT
                startListening()
            } else {
                // 2회 연속 실패: STT 종료, 웨이크워드만 재개
                consecutiveMiss = 0
                resumeWakewordWithDelay()
            }
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
    // Porcupine (웨이크워드)
    // ────────────────────────────
    private fun initPorcupine() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO not granted for Porcupine")
            return
        }

        val callback = PorcupineManagerCallback { _ ->
            if (isListening) return@PorcupineManagerCallback
            Handler(Looper.getMainLooper()).post {
                try {
                    if (isListening) return@post
                    playEarcon()
                    safeStopWakeword()
                    startListening()
                } catch (t: Throwable) {
                    Log.e(TAG, "Wakeword handling failed", t)
                    resumeWakewordWithDelay()
                }
            }
        }

        porcupineManager = PorcupineManager.Builder()
            .setAccessKey(pvAccessKey)
            .setKeywordPath(kwPath)          // jenny_ko_android_v3_0_0.ppn
            .setModelPath(modelPath)         // porcupine_params_ko.pv
            .setSensitivity(0.55f)           // 민감도
            .setErrorCallback(
                 { e ->
                    Log.e(TAG, "Porcupine error", e)
                    Handler(Looper.getMainLooper()).post {
                        safeStopWakeword()
                        resumeWakewordWithDelay(300)
                    }
                }
            )
            .build(applicationContext, callback)

        Log.i(TAG, "Porcupine initialized (ko model, sensitivity=0.55)")
    }

    private fun startWakeword() {
        if (porcupineManager == null || isWakewordActive) return
        try {
            porcupineManager!!.start()
            isWakewordActive = true
            Log.d(TAG, "Porcupine listening")
        } catch (e: Exception) {
            Log.e(TAG, "Porcupine start failed", e)
        }
    }

    private fun safeStopWakeword() {
        if (porcupineManager == null || !isWakewordActive) return
        try {
            porcupineManager!!.stop()
            isWakewordActive = false
            Log.d(TAG, "Porcupine stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Porcupine stop failed", e)
        }
    }

    private fun resumeWakewordWithDelay(delayMs: Long = 200L) {
        Handler(Looper.getMainLooper()).postDelayed({ startWakeword() }, delayMs)
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

    // ────────────────────────────
    // assets → files 복사 (경로 문자열 필요)
    // ────────────────────────────
    private fun copyAssetOnce(name: String): String {
        val out = java.io.File(filesDir, name)
        if (!out.exists()) {
            assets.open(name).use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return out.absolutePath
    }
}
