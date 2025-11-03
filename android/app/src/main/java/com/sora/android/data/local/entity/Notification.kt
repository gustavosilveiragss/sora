package com.sora.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["recipientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["recipientId"]),
        Index(value = ["type"]),
        Index(value = ["isRead"])
    ]
)
data class Notification(
    @PrimaryKey val id: Long,
    val recipientId: Long,
    val type: String,
    val triggerUserId: Long? = null,
    val triggerUserUsername: String? = null,
    val triggerUserFirstName: String? = null,
    val triggerUserLastName: String? = null,
    val triggerUserProfilePicture: String? = null,
    val postId: Long? = null,
    val postCityName: String? = null,
    val postThumbnailUrl: String? = null,
    val commentPreview: String? = null,
    val isRead: Boolean = false,
    val createdAt: String,
    val cacheTimestamp: Long = System.currentTimeMillis()
)