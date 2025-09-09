package com.example.eeum.data.model.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NewActionResult(
    val room: String,
    val device: String,
    val state: String,      // "켜기" / "끄기"
    val windLevel: Int?,    // 선풍기일 때만
    val acTemp: Int?        // 에어컨일 때만
) : Parcelable
