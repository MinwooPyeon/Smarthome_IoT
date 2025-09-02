//package com.example.mon_fit.data.remote
//
//import okhttp3.Interceptor
//import okhttp3.Response
//import com.example.mon_fit.base.ApplicationClass
//
//class AuthInterceptor : Interceptor {
//    override fun intercept(chain: Interceptor.Chain): Response {
//        val sp = ApplicationClass.sharedPreferencesUtil
//        val token = sp.getAccessToken()
//        val builder = chain.request().newBuilder().apply {
//            if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token")
//        }.build()
//
//        val res = chain.proceed(builder)
//
//        if (res.code == 401) {
//            ApplicationClass.goLoginFromAnywhere() // ✅ 한 줄 추가
//        }
//        return res
//    }
//}