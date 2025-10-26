package com.sora.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "post_media",
    foreignKeys = [
        ForeignKey(
            entity = Post::class,
            parentColumns = ["id"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["postId"]),
        Index(value = ["sortOrder"])
    ]
)
data class PostMedia(
    @PrimaryKey val id: Long,
    val postId: Long,
    val fileName: String,
    val cloudinaryPublicId: String,
    val cloudinaryUrl: String,
    val mediaType: String,
    val fileSize: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val sortOrder: Int = 0,
    val cacheTimestamp: Long = System.currentTimeMillis()
)