package com.example.eeum.data.remote

import com.example.eeum.data.remote.service.AuthService
import com.example.eeum.data.remote.service.DeviceService
import com.example.eeum.data.remote.service.EnergyService
import com.example.eeum.data.remote.service.FloorplansService
import com.example.eeum.data.remote.service.RoutineService
import com.example.eeum.data.remote.service.UserService
import com.example.eeum.base.ApplicationClass
import com.example.eeum.data.remote.service.HomeService

class RetrofitUtil {
    companion object{
        val authService = ApplicationClass.retrofit.create(AuthService::class.java)
        val userService = ApplicationClass.retrofit.create(UserService::class.java)
        val deviceService = ApplicationClass.retrofit.create(DeviceService::class.java)
        val energyService = ApplicationClass.retrofit.create(EnergyService::class.java)
        val routineService = ApplicationClass.retrofit.create(RoutineService::class.java)
        val floorplansService = ApplicationClass.retrofit.create(FloorplansService::class.java)
        val homeService = ApplicationClass.retrofit.create(HomeService::class.java)

    }
}