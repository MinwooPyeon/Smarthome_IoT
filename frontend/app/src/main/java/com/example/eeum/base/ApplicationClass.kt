//package com.example.mon_fit.base
//
//import android.app.Activity
//import android.app.Application
//import android.app.Dialog
//import android.graphics.Color
//import android.graphics.drawable.ColorDrawable
//import android.os.Bundle
//import android.view.LayoutInflater
//import com.example.mon_fit.data.SharedPreferencesUtil
//import com.example.mon_fit.data.remote.AuthInterceptor
//import com.example.mon_fit.data.remote.RefreshApi
//import com.example.mon_fit.data.remote.TokenAuthenticator
//import com.example.mon_fit.databinding.DialogLoadingBinding
//import com.google.gson.Gson
//import com.google.gson.GsonBuilder
//import okhttp3.OkHttpClient
//import okhttp3.logging.HttpLoggingInterceptor
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import java.util.concurrent.TimeUnit
//
//private const val TAG = "ApplicationClass_싸피"
//
//class ApplicationClass : Application(), Application.ActivityLifecycleCallbacks {
//
//    companion object {
//        // 서버 베이스 URL (RetrofitUtil이 이 값을 사용)
//        const val SERVER_URL = "http://i13d102.p.ssafy.io:8088/"
//
//        lateinit var sharedPreferencesUtil: SharedPreferencesUtil
//            private set
//        lateinit var retrofit: Retrofit
//
//        // 로딩 관련
//        private var currentActivity: Activity? = null
//        private var loadingDialog: Dialog? = null
//
//        fun showLoading(message: String = "로딩 중...") {
//            hideLoading()
//            currentActivity?.let { activity ->
//                runCatching {
//                    val binding = DialogLoadingBinding.inflate(LayoutInflater.from(activity))
//                    binding.tvLoadingMessage.text = message
//                    loadingDialog = Dialog(activity).apply {
//                        setContentView(binding.root)
//                        setCancelable(false)
//                        setCanceledOnTouchOutside(false)
//                        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//                        show()
//                    }
//                }.onFailure { it.printStackTrace() }
//            }
//        }
//
//
//
//        fun hideLoading() {
//            runCatching {
//                loadingDialog?.dismiss()
//                loadingDialog = null
//            }.onFailure { it.printStackTrace() }
//        }
//
//        fun isLoading(): Boolean = loadingDialog?.isShowing == true
//
//
//        //401(토큰 만료 시)
//        @Volatile private var navigatingToLogin = false
//
//        fun goLoginFromAnywhere() {
//            if (navigatingToLogin) return
//            navigatingToLogin = true
//
//            // 세션 정리
//            sharedPreferencesUtil.clearAuthSession()
//
//            val activity = currentActivity ?: run {
//                // 포그라운드 Activity 없으면 다음에 복귀 시 MainActivity의 세션검사에서 걸리게 함
//                navigatingToLogin = false
//                return
//            }
//
//            // 이미 로그인 화면이면 무시
//            if (activity is com.example.mon_fit.ui.login.LoginActivity) {
//                navigatingToLogin = false
//                return
//            }
//
//            activity.runOnUiThread {
//                try {
//                    val intent = android.content.Intent(activity, com.example.mon_fit.ui.login.LoginActivity::class.java).apply {
//                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
//                        putExtra("dest", "signin")
//                    }
//                    activity.startActivity(intent)
//                } finally {
//                    navigatingToLogin = false
//                }
//            }
//        }
//    }
//
//    // 다른 곳에서 필요하면 사용
//    val gson: Gson = GsonBuilder()
//        .setLenient()
//        .serializeNulls()
//        .create()
//
//    override fun onCreate() {
//        super.onCreate()
//        registerActivityLifecycleCallbacks(this)
//
//        // SharedPreferences 초기화
//        sharedPreferencesUtil = SharedPreferencesUtil(applicationContext)
//
//        // --- 로깅 인터셉터 ---
//        val logging = HttpLoggingInterceptor().apply {
//            level = HttpLoggingInterceptor.Level.BODY
//        }
//
//        // --- refresh 전용 Retrofit/Api (옵션: 토큰 재발급용) ---
//        val refreshClient = OkHttpClient.Builder()
//            .addInterceptor(logging)
//            .connectTimeout(30, TimeUnit.SECONDS)
//            .readTimeout(30, TimeUnit.SECONDS)
//            .build()
//
//        val refreshRetrofit = Retrofit.Builder()
//            .baseUrl(SERVER_URL)
//            .client(refreshClient)
//            .addConverterFactory(GsonConverterFactory.create(gson))
//            .build()
//
//        // 같은 패키지(com.example.mon_fit.base)에 선언된 인터페이스/데이터클래스 사용
//        val refreshApi: RefreshApi = refreshRetrofit.create(RefreshApi::class.java)
//
//        // --- 앱 공용 OkHttpClient (Authorization + 401 재발급 + 로깅) ---
//        val okHttpClient = OkHttpClient.Builder()
//            .addInterceptor(AuthInterceptor())                 // Bearer {accessToken} 자동 첨부
//           // .authenticator(TokenAuthenticator(refreshApi))     // 401 시 refresh 토큰으로 재발급 (서버 지원 시)
//            .addInterceptor(logging)
//            .connectTimeout(30, TimeUnit.SECONDS)
//            .readTimeout(30, TimeUnit.SECONDS)
//            .build()
//
//        // --- 앱 공용 Retrofit (RetrofitUtil이 참조) ---
//        retrofit = Retrofit.Builder()
//            .baseUrl(SERVER_URL)
//            .addConverterFactory(GsonConverterFactory.create(gson))
//            .client(okHttpClient)
//            .build()
//    }
//
//    // Activity 생명주기 콜백들
//    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
//    override fun onActivityStarted(activity: Activity) {}
//    override fun onActivityResumed(activity: Activity) { currentActivity = activity }
//    override fun onActivityPaused(activity: Activity) { if (currentActivity == activity) currentActivity = null }
//    override fun onActivityStopped(activity: Activity) { if (currentActivity == activity) hideLoading() }
//    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
//    override fun onActivityDestroyed(activity: Activity) {
//        if (currentActivity == activity) {
//            hideLoading()
//            currentActivity = null
//        }
//    }
//}
