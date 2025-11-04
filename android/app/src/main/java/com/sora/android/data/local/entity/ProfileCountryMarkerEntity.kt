package com.sora.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "profile_country_marker",
    primaryKeys = ["userId", "countryCode"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileGlobeDataEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ProfileCountryMarkerEntity(
    val userId: Long,
    val countryCode: String,
    val countryNameKey: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val recentPostsCount: Int,
    val lastPostDate: String? = null
)