package com.sora.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "like_post",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Post::class,
            parentColumns = ["id"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId", "postId"], unique = true),
        Index(value = ["userId"]),
        Index(value = ["postId"])
    ]
)
data class LikePost(
    @PrimaryKey val id: Long,
    val userId: Long,
    val postId: Long,
    val createdAt: String,
    val cacheTimestamp: Long = System.currentTimeMillis()
)