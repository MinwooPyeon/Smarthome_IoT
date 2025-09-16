package com.example.eeum.data.remote.repository

import com.example.eeum.base.RoutineDirectoryCache
import com.example.eeum.data.model.dto.routine.RoutineRequest
import com.example.eeum.data.model.response.common.ApiResponse
import com.example.eeum.data.model.response.common.BaseResponse
import com.example.eeum.data.model.response.routine.RoutineCreateResponse
import com.example.eeum.data.model.response.routine.RoutineResponse
import com.example.eeum.data.remote.service.RoutineService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class RoutineRepository(
    private val service: RoutineService,
    private val dir: RoutineDirectoryCache? = null
) {
    suspend fun createRoutine(req: RoutineRequest): Result<Int> = withContext(Dispatchers.IO) {
        val res: Response<ApiResponse<RoutineCreateResponse>> = service.createRoutine(req)
        if (!res.isSuccessful) return@withContext fail("HTTP ${res.code()} ${res.message()}")
        val api = res.body() ?: return@withContext fail("빈 응답")
        if (api.status != "SUCCESS") return@withContext fail("등록 실패")

        val id = api.data.routineId
        dir?.upsert(req.name, id)
        Result.success(id)
    }

    suspend fun readRoutineById(id: Int): Result<RoutineResponse> = withContext(Dispatchers.IO) {
        val res = service.readRoutine(id)
        if (!res.isSuccessful) return@withContext fail(httpMsg(res))
        val api = res.body() ?: return@withContext fail("빈 응답")
        if (api.status != "SUCCESS") return@withContext fail(apiError(res, "조회 실패"))
        Result.success(api.data)
    }

    fun findIdByName(name: String?): Int? =
        name?.trim()?.takeIf { it.isNotEmpty() }?.let { n -> dir?.findIdByName(n) }

    suspend fun deleteRoutineById(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val res = service.deleteRoutine(id)
        if (!res.isSuccessful) return@withContext fail(httpMsg(res))
        val api = res.body() ?: return@withContext fail("빈 응답")
        if (api.status != "SUCCESS") return@withContext fail(apiError(res, "삭제 실패"))
        Result.success(Unit)
    }

    private fun <T> fail(msg: String): Result<T> = Result.failure(IllegalStateException(msg))

    private fun httpMsg(res: retrofit2.Response<*>): String =
        "HTTP ${res.code()} ${res.message()}".trim()

    // {"status":"FAIL","error":"..."} 형태면 error를 뽑아 친절화
    private fun apiError(res: retrofit2.Response<*>, fallback: String): String {
        val raw = res.errorBody()?.string().orEmpty()
        return try {
            val err = org.json.JSONObject(raw).optString("error")
            if (err.isNullOrBlank()) fallback else err
        } catch (_: Exception) { fallback }
    }
}