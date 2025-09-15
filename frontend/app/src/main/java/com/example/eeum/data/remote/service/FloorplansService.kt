package com.example.eeum.data.remote.service

import com.example.eeum.data.model.response.floorplans.MapData
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface FloorplansService {
    @GET("/api/addresses/search")
    suspend fun searchHouses(
        @Query("keyword") keyword: String?
    ): Response<MapData>
}