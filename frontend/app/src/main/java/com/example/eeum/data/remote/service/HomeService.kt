package com.example.eeum.data.remote.service

import com.example.eeum.data.model.response.home.AllUserHome
import com.example.eeum.data.model.response.home.GetPrimaryHome
import com.example.eeum.data.model.response.home.PrimaryHome
import com.example.eeum.data.model.response.routine.AllRoom
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface HomeService {

    //유저 집 목록 조회
    @GET("/api/user-home/address")
    suspend fun getUserHomes(): Response<AllUserHome>

    // 대표 집 조회
    @GET("/api/user-home/address/primary")
    suspend fun getPrimaryHome(): Response<GetPrimaryHome>

    // 대표집 수정
    @PUT("/api/user-home/{homeId}/primary")
    suspend fun setPrimaryHome(
        @Path("homeId") homeId: Int
    ): Response<PrimaryHome>

    // 홈의 방 목록 조회
    @GET("api/user-home/{homeId}/rooms")
    suspend fun readRooms(
        @Path("homeId") homeId: Int
    ): Response<AllRoom>
}
