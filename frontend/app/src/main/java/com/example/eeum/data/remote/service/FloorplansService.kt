package com.example.eeum.data.remote.service

import com.example.eeum.data.model.response.floorplans.HouseFloorPlans
import com.example.eeum.data.model.response.floorplans.MapData
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FloorplansService {
    @GET("/api/addresses/search")
    suspend fun searchHouses(
        @Query("keyword") keyword: String?
    ): Response<MapData>

    // 주소별 평면도 목록 조회
    @GET("/api/addresses/{addressId}/floorplans")
    suspend fun getFloorplans(
        @Path("addressId") addressId: Int
    ): Response<HouseFloorPlans>
}