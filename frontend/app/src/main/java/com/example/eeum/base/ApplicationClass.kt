package com.example.eeum.base

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import com.example.eeum.data.SharedPreferencesUtil
import com.example.eeum.data.remote.service.AuthService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.naver.maps.map.NaverMapSdk
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val TAG = "EEUM_ApplicationClass"

class ApplicationClass : Application(), Application.ActivityLifecycleCallbacks {

    companion object {
        const val SERVER_URL = "http://43.201.62.254:80/"

        lateinit var sharedPreferencesUtil: SharedPreferencesUtil
            private set
        lateinit var retrofit: Retrofit

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

        // ✅ 네이버 맵 SDK Client ID를 Manifest 메타데이터에서 읽어 주입
        initNaverMapClient()
        val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        android.util.Log.d("NaverMapCheck", "pkg=" + packageName)
        android.util.Log.d("NaverMapCheck", "clientId(from Manifest)=" + ai.metaData?.getString("com.naver.maps.map.CLIENT_ID"))


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
    }

    /** Manifest의 <meta-data android:name="com.naver.maps.map.CLIENT_ID"> 값을 읽어 SDK에 주입 */
    private fun initNaverMapClient() {
        val clientId = runCatching {
            val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            ai.metaData?.getString("com.naver.maps.map.CLIENT_ID")
        }.getOrNull()

        if (clientId.isNullOrBlank()) {
            Log.e(TAG, "NaverMap CLIENT_ID not found in Manifest meta-data.")
            return
        }

        NaverMapSdk.getInstance(this).client =
            NaverMapSdk.NaverCloudPlatformClient(clientId)

        Log.d(TAG, "NaverMap CLIENT_ID set: $clientId")
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
}
