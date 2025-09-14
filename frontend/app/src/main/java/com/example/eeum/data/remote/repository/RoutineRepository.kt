package com.example.eeum.data.remote.repository

import com.example.eeum.data.model.dto.routine.RoutineRequest
import com.example.eeum.data.model.response.common.ApiResponse
import com.example.eeum.data.model.response.common.BaseResponse
import com.example.eeum.data.remote.service.RoutineService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class RoutineRepository(
    private val service: RoutineService
) {
    suspend fun createRoutine(req: RoutineRequest): Result<String> = withContext(Dispatchers.IO) {
        val res: Response<ApiResponse<BaseResponse>> = service.createRoutine(req)
        if (!res.isSuccessful) return@withContext fail("HTTP ${res.code()} ${res.message()}")
        val api = res.body() ?: return@withContext fail("빈 응답")
        if (api.status != "SUCCESS") return@withContext fail("등록 실패")
        Result.success(api.data.id)
    }

    private fun <T> fail(msg: String): Result<T> = Result.failure(IllegalStateException(msg))
}