package com.sora.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class User(
    @PrimaryKey val id: Long,
    val username: String,
    val email: String? = null,
    val firstName: String,
    val lastName: String,
    val bio: String? = null,
    val profilePicture: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val countriesVisitedCount: Int = 0,
    val cacheTimestamp: Long = System.currentTimeMillis(),
    val isFullProfile: Boolean = false
)