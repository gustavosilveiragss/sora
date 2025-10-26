package com.sora.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.sora.android.data.local.converter.StringListConverter

@Entity(tableName = "post")
@TypeConverters(StringListConverter::class)
data class Post(
    @PrimaryKey val id: Long,
    val authorId: Long,
    val authorUsername: String,
    val authorProfilePicture: String? = null,
    val profileOwnerId: Long,
    val profileOwnerUsername: String? = null,
    val countryId: Long,
    val countryCode: String,
    val collectionId: Long? = null,
    val collectionCode: String,
    val cityName: String,
    val cityLatitude: Double? = null,
    val cityLongitude: Double? = null,
    val caption: String? = null,
    val mediaUrls: List<String> = emptyList(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLikedByCurrentUser: Boolean = false,
    val createdAt: String,
    val updatedAt: String? = null,
    val visibilityType: String = "PERSONAL", // PERSONAL, SHARED, COLLABORATIVE
    val sharedPostGroupId: String? = null,
    val cacheTimestamp: Long = System.currentTimeMillis()
)