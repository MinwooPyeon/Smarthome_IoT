package com.example.eeum.base

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import com.example.eeum.data.remote.service.AuthService
import com.example.eeum.util.NotificationUtil
import com.example.eeum.util.SharedPreferencesUtil
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.naver.maps.map.NaverMapSdk
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import coil.ImageLoader
import coil.Coil
import android.os.Build
import java.security.MessageDigest
import com.example.eeum.BuildConfig

private const val TAG = "EEUM_ApplicationClass"

class ApplicationClass : Application(), Application.ActivityLifecycleCallbacks {

    companion object {
        const val SERVER_URL = "http://43.201.62.254:80/"

        lateinit var sharedPreferencesUtil: SharedPreferencesUtil
            private set
        lateinit var retrofit: Retrofit
        lateinit var imageLoader: ImageLoader

        // 로딩 관련
        private var currentActivity: Activity? = null
        private var loadingDialog: Dialog? = null

        fun hideLoading() {
            runCatching {
                loadingDialog?.dismiss()
                loadingDialog = null
            }.onFailure { it.printStackTrace() }
        }

        fun isLoading(): Boolean = loadingDialog?.isShowing == true

        //401(토큰 만료 시)
        @Volatile private var navigatingToLogin = false

        fun goLoginFromAnywhere() {
            if (navigatingToLogin) return
            navigatingToLogin = true

            // 세션 정리
            sharedPreferencesUtil.clearAuthSession()

            val activity = currentActivity ?: run {
                navigatingToLogin = false
                return
            }

            // 로그인 이동 로직 필요 시 여기에…
        }
    }

    // 다른 곳에서 필요하면 사용
    val gson: Gson = GsonBuilder()
        .setLenient()
        .serializeNulls()
        .create()

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
        com.jakewharton.threetenabp.AndroidThreeTen.init(this)
        NotificationUtil.ensureChannels(this)

        // 네이버 맵 SDK Client ID를 Manifest 메타데이터에서 읽어 주입
        initNaverMapClient()
        val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        Log.d("NaverMapCheck", "pkg=" + packageName)
        Log.d("NaverMapCheck", "NCP_KEY_ID(manifest)=" + ai.metaData?.getString("com.naver.maps.map.NCP_KEY_ID"))
        logAppSigningSha1()


        // SharedPreferences 초기화
        sharedPreferencesUtil = SharedPreferencesUtil(applicationContext)

        // --- 로깅 인터셉터 ---
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // --- refresh 전용 Retrofit/Api (옵션: 토큰 재발급용) ---
        val refreshClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val refreshRetrofit = Retrofit.Builder()
            .baseUrl(SERVER_URL)
            .client(refreshClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val authService: AuthService = refreshRetrofit.create(AuthService::class.java)

        // --- 앱 공용 OkHttpClient (Authorization + 401 재발급 + 로깅) ---
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())                 // Bearer {accessToken} 자동 첨부
            //.authenticator(TokenAuthenticator(refreshApi))   // 401 시 refresh 토큰 재발급(서버 지원 시)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        // --- 앱 공용 Retrofit (RetrofitUtil이 참조) ---
        retrofit = Retrofit.Builder()
            .baseUrl(SERVER_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()

        // --- Coil ImageLoader (AsyncImage에서 인증 헤더 포함하여 사용) ---
        imageLoader = ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)
    }
    private fun initNaverMapClient() {
        // 1) Manifest에서 키 조회
        val manifestClientId = runCatching {
            val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            ai.metaData?.getString("com.naver.maps.map.NCP_KEY_ID")
        }.getOrNull()

        // 2) 실패 시 BuildConfig에서 가져오기
        val clientId = manifestClientId ?: runCatching { BuildConfig.NAVER_CLIENT_ID }.getOrNull()

        if (clientId.isNullOrBlank()) {
            Log.e(TAG, "NaverMap CLIENT_ID not configured. Check Manifest placeholders or BuildConfig.")
            return
        }

        Log.d(TAG, "NaverMap CLIENT_ID (from manifest): $clientId - SDK will use meta-data automatically")
    }

    // Activity 생명주기 콜백들
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) { currentActivity = activity }
    override fun onActivityPaused(activity: Activity) { if (currentActivity == activity) currentActivity = null }
    override fun onActivityStopped(activity: Activity) { if (currentActivity == activity) hideLoading() }
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            hideLoading()
            currentActivity = null
        }
    }

    private fun logAppSigningSha1() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val pkgInfo = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                val certs = pkgInfo.signingInfo?.apkContentsSigners
                if (!certs.isNullOrEmpty()) {
                    val sha1 = sha1Of(certs[0].toByteArray())
                    Log.d(TAG, "App SHA-1 (first signer): $sha1")
                }
            } else {
                @Suppress("DEPRECATION")
                val pkgInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                val sigs = pkgInfo.signatures
                if (!sigs.isNullOrEmpty()) {
                    @Suppress("DEPRECATION")
                    val sha1 = sha1Of(sigs[0].toByteArray())
                    Log.d(TAG, "App SHA-1 (first signer): $sha1")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log app signing SHA-1", e)
        }
    }

    private fun sha1Of(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(bytes)
        return digest.joinToString(":") { b -> "%02X".format(b) }
    }
}
