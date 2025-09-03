package com.example.eeum.voice

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

interface ChatClient { suspend fun ask(prompt: String): String }

class HttpChatClient(private val url: String, private val apiKey: String) : ChatClient {
    private val http = OkHttpClient()
    override suspend fun ask(prompt: String): String = withContext(Dispatchers.IO) {
        val body = """
      {"model":"gpt-4o-mini","messages":[{"role":"user","content":${JSONObject.quote(prompt)}}]}
    """.trimIndent()
        val req = Request.Builder().url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(req).execute().use { res ->
            if (!res.isSuccessful) error("LLM ${res.code}")
            // 공급자 응답 포맷에 맞게 파싱
            JSONObject(res.body!!.string())
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
        }
    }
}