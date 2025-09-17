package com.example.eeum.data.remote.service

import com.example.eeum.data.model.response.home.AllUserHome
import com.example.eeum.data.model.response.home.PrimaryHome
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface HomeService {

    //유저 집 목록 조회
    @GET("/api/user-home/address")
    suspend fun getUserHomes(): Response<AllUserHome>

    @PUT("/api/user-home/{homeId}/primary")
    suspend fun setPrimaryHome(
        @Path("homeId") homeId: Int
    ): Response<PrimaryHome>
}
