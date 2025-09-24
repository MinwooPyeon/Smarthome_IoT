package com.example.eeum.data.remote.service

import com.example.eeum.data.model.response.menu.UserResponse
import retrofit2.Call
import retrofit2.http.GET

interface UserService {
    
    //내 정보 조회
    @GET("/api/user")
    fun getUserInfo(): Call<UserResponse>
}
