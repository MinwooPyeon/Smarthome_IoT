package com.example.eeum.base

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val sp = ApplicationClass.sharedPreferencesUtil
        val token = sp.getAccessToken()
        val builder = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token")
        }.build()

        val res = chain.proceed(builder)

        if (res.code == 401) {
            ApplicationClass.goLoginFromAnywhere()
        }
        return res
    }
}
