package com.example.eeum.data.remote

interface DeviceApi {
    fun turnOn(device: String, room: String)
    fun turnOff(device: String, room: String)
    fun setLevel(device: String, room: String, level: Int)
}