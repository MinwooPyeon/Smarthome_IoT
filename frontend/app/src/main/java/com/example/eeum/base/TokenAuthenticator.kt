package com.example.eeum.base

import com.example.eeum.data.remote.service.AuthService
import okhttp3.*

class TokenAuthenticator(
    private val authService: AuthService
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // 무한 루프 방지
        if (responseCount(response) >= 2) return null

        val sp = ApplicationClass.sharedPreferencesUtil
        val refreshToken = sp.getRefreshToken() ?: return null

        // 동기 호출로 새 토큰 요청
        val newToken = try {
            val res = authService.refresh("Bearer $refreshToken").execute()
            if (res.isSuccessful) {
                val body = res.body() ?: return null
                // 서버 스펙에 맞게 파싱
                val accessToken = body.accessToken
                sp.setAccessToken(accessToken)
                accessToken
            } else null
        } catch (_: Exception) { null }

        if (newToken.isNullOrBlank()) return null

        // 실패했던 요청을 새 토큰으로 재시도
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var r: Response? = response
        var count = 1
        while (r?.priorResponse != null) {
            count++
            r = r.priorResponse
        }
        return count
    }
}

