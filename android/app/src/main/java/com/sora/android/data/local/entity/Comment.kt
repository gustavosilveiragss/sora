package com.sora.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "comment",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["authorId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Post::class,
            parentColumns = ["id"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Comment::class,
            parentColumns = ["id"],
            childColumns = ["parentCommentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["postId"]),
        Index(value = ["authorId"]),
        Index(value = ["parentCommentId"])
    ]
)
data class Comment(
    @PrimaryKey val id: Long,
    val authorId: Long,
    val authorUsername: String,
    val authorProfilePicture: String? = null,
    val postId: Long,
    val parentCommentId: Long? = null,
    val content: String,
    val repliesCount: Int = 0,
    val isLikedByCurrentUser: Boolean = false,
    val createdAt: String,
    val updatedAt: String? = null,
    val cacheTimestamp: Long = System.currentTimeMillis()
)