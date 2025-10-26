package com.sora.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "follow",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["followerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["followingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["followerId", "followingId"], unique = true),
        Index(value = ["followerId"]),
        Index(value = ["followingId"])
    ]
)
data class Follow(
    @PrimaryKey val id: Long,
    val followerId: Long,
    val followingId: Long,
    val createdAt: String,
    val cacheTimestamp: Long = System.currentTimeMillis()
)