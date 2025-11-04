package com.sora.android.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserPostLocationDto(
    @SerializedName("postId")
    val postId: Long,
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("thumbnailUrl")
    val thumbnailUrl: String
)