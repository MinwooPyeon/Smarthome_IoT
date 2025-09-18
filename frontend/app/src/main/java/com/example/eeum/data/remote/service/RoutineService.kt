package com.example.eeum.data.remote.service

import com.example.eeum.data.model.dto.routine.RoutineRequest
import com.example.eeum.data.model.response.common.ApiResponse
import com.example.eeum.data.model.response.common.BaseResponse
import com.example.eeum.data.model.response.common.Page
import com.example.eeum.data.model.response.routine.AllRoutine
import com.example.eeum.data.model.response.routine.RoutineCreateResponse
import com.example.eeum.data.model.response.routine.RoutineIcon
import com.example.eeum.data.model.response.routine.RoutineResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface RoutineService {

    // 루틴 등록
    @POST("api/routines")
    suspend fun createRoutine(
        @Body body: RoutineRequest
    ): Response<ApiResponse<RoutineCreateResponse>>

    // 루틴 조회
    @GET("api/routines")
    suspend fun readRoutines(
        @Query("mask") mask: Int? = null,
    ): Response<ApiResponse<Page<RoutineResponse>>>

    //루틴 전체 조회
    @GET("api/routines")
    suspend fun readAllRoutines(): Response<AllRoutine>

    // 루틴 아이콘 목록 조회
    @GET("api/icons")
    suspend fun readRoutineIcons(): Response<RoutineIcon>

    // 루틴 단건 조회
    @GET("api/routines/{routineId}")
    suspend fun readRoutine(
        @Path("routineId") id: Int,
    ): Response<ApiResponse<RoutineResponse>>

    // 루틴 수정
    @PUT("api/routines/{routineId}")
    suspend fun updateRoutine(
        @Path("routineId") id: Int,
    ): Response<ApiResponse<BaseResponse>>

    // 루틴 삭제
    @DELETE("api/routines/{routineId}")
    suspend fun deleteRoutine(
        @Path("routineId") id: Int,
    ): Response<ApiResponse<BaseResponse>>

}