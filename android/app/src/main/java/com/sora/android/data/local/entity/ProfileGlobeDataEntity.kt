package com.sora.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile_globe_data")
data class ProfileGlobeDataEntity(
    @PrimaryKey
    val userId: Long,
    val totalCountriesWithActivity: Int,
    val totalRecentPosts: Int,
    val lastUpdated: String,
    val cacheTimestamp: Long
)