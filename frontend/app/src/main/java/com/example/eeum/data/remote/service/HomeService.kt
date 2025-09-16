package com.example.eeum.data.remote.service

import com.example.eeum.data.model.response.home.AllUserHome
import retrofit2.Response
import retrofit2.http.GET

interface HomeService {

    //유저 집 목록 조회
    @GET("/api/user-home/address")
    suspend fun getUserHomes(): Response<AllUserHome>
}
