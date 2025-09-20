package com.example.eeum.ui.screens

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RoomDataP(
    val roomColor: String,
    val roomId: Int,
    val roomName: String
) : Parcelable

@Parcelize
data class DeviceItemP(
    val brand: String,
    val deviceId: Int,
    val deviceName: String,
    val deviceType: String,
    val irDeviceId: String,
    val model: String,
    val registeredAt: String,
    val roomId: Int,
    val x: Double,
    val y: Double
) : Parcelable

@Parcelize
data class ActionAddedPayload(
    val room: RoomDataP,
    val device: DeviceItemP,
    val stateTitle: String,
    val power: Boolean,      // true=켜기, false=끄기
    val windLevel: Int?,     // 선풍기/공청기/에어컨
    val acTemp: Int?         // 에어컨
) : Parcelable
