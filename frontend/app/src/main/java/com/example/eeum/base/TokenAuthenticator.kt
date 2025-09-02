//// com.example.mon_fit.data.remote.TokenAuthenticator.kt
//package com.example.mon_fit.data.remote
//
//import okhttp3.*
//import com.example.mon_fit.base.ApplicationClass
//import retrofit2.http.Header
//import retrofit2.http.POST
//
//class TokenAuthenticator(
//    private val refreshApi: RefreshApi // refresh 토큰으로 재발급하는 Retrofit API
//) : Authenticator {
//
//    override fun authenticate(route: Route?, response: Response): Request? {
//        // 무한 루프 방지
//        if (responseCount(response) >= 2) return null
//
//        val sp = ApplicationClass.sharedPreferencesUtil
//        val refreshToken = sp.getRefreshToken() ?: return null
//
//        // 동기 호출로 새 토큰 요청
//        val newToken = try {
//            val res = refreshApi.refresh("Bearer $refreshToken").execute()
//            if (res.isSuccessful) {
//                val body = res.body() ?: return null
//                // 서버 스펙에 맞게 파싱
//                val accessToken = body.accessToken
//                sp.setAccessToken(accessToken)
//                accessToken
//            } else null
//        } catch (_: Exception) { null }
//
//        if (newToken.isNullOrBlank()) return null
//
//        // 실패했던 요청을 새 토큰으로 재시도
//        return response.request.newBuilder()
//            .header("Authorization", "Bearer $newToken")
//            .build()
//    }
//
//    private fun responseCount(response: Response): Int {
//        var r: Response? = response
//        var count = 1
//        while (r?.priorResponse != null) {
//            count++
//            r = r.priorResponse
//        }
//        return count
//    }
//}
//
//// 예시: refresh API 인터페이스
//interface RefreshApi {
//    @POST("api/auth/refresh")
//    fun refresh(@Header("Authorization") refreshBearer: String): retrofit2.Call<RefreshResponse>
//}
//data class RefreshResponse(val accessToken: String)
