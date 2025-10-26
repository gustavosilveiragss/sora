package com.sora.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_user_stats")
data class CachedUserStats(
    @PrimaryKey
    val userId: Long,
    val username: String,
    val totalCountriesVisited: Int,
    val totalCitiesVisited: Int,
    val totalPostsCount: Int,
    val totalLikesReceived: Int,
    val totalCommentsReceived: Int,
    val totalFollowers: Int,
    val totalFollowing: Int,
    val cacheTimestamp: Long
)
