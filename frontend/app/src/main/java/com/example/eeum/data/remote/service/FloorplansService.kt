package com.example.eeum.data.remote.service

import com.example.eeum.data.model.response.floorplans.FloorPlan
import com.example.eeum.data.model.response.floorplans.HouseFloorPlans
import com.example.eeum.data.model.response.floorplans.MapData
import com.example.eeum.data.model.response.floorplans.RegisterHome
import retrofit2.Response
import retrofit2.http.*

interface FloorplansService {

    // 주소 검색
    @GET("/api/addresses/search")
    suspend fun searchHouses(
        @Query("keyword") keyword: String?
    ): Response<MapData>

    // 주소별 평면도 목록 조회
    @GET("/api/addresses/{addressId}/floorplans")
    suspend fun getFloorplans(
        @Path("addressId") addressId: Int
    ): Response<HouseFloorPlans>

    // 평면도 등록
    @POST("/api/homes/{homeId}/floorplans")
    suspend fun uploadFloorplan(
        @Path("homeId") homeId: Int,
    ): Response<RegisterHome>

    // 특정 평면도 검색
    @GET("/api/users/homes/{homeId}/floorplans")
    suspend fun getUserHomeFloorplans(
        @Path("homeId") homeId: Int
    ): Response<FloorPlan>
}
