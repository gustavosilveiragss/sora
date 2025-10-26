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
    val type: String, // NEW_FOLLOWER, POST_LIKED, POST_COMMENTED, TRAVEL_PERMISSION_INVITATION, etc.
    val title: String,
    val message: String,
    val referenceId: String? = null,
    val referenceType: String? = null,
    val isRead: Boolean = false,
    val createdAt: String,
    val cacheTimestamp: Long = System.currentTimeMillis()
)