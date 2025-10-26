package com.sora.android.data.local.dao

import androidx.room.*
import com.sora.android.data.local.entity.PostMedia
import kotlinx.coroutines.flow.Flow

@Dao
interface PostMediaDao {

    @Query("SELECT * FROM post_media WHERE postId = :postId ORDER BY sortOrder ASC")
    suspend fun getPostMedia(postId: Long): List<PostMedia>

    @Query("SELECT * FROM post_media WHERE postId = :postId ORDER BY sortOrder ASC")
    fun getPostMediaFlow(postId: Long): Flow<List<PostMedia>>

    @Query("SELECT * FROM post_media WHERE id = :mediaId")
    suspend fun getMediaById(mediaId: Long): PostMedia?

    @Query("SELECT * FROM post_media WHERE cloudinaryPublicId = :publicId")
    suspend fun getMediaByPublicId(publicId: String): PostMedia?

    @Query("SELECT COUNT(*) FROM post_media WHERE postId = :postId")
    suspend fun getPostMediaCount(postId: Long): Int

    @Query("SELECT COUNT(*) FROM post_media WHERE postId = :postId")
    fun getPostMediaCountFlow(postId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM post_media WHERE mediaType = :mediaType")
    suspend fun getMediaCountByType(mediaType: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: PostMedia)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaList(mediaList: List<PostMedia>)

    @Update
    suspend fun updateMedia(media: PostMedia)

    @Delete
    suspend fun deleteMedia(media: PostMedia)

    @Query("DELETE FROM post_media WHERE id = :mediaId")
    suspend fun deleteMediaById(mediaId: Long)

    @Query("DELETE FROM post_media WHERE postId = :postId")
    suspend fun deletePostMedia(postId: Long)

    @Query("UPDATE post_media SET sortOrder = :sortOrder WHERE id = :mediaId")
    suspend fun updateMediaSortOrder(mediaId: Long, sortOrder: Int)

    @Query("DELETE FROM post_media WHERE cacheTimestamp < :expiry")
    suspend fun deleteExpiredMedia(expiry: Long): Int

    @Query("UPDATE post_media SET cacheTimestamp = :timestamp WHERE id = :mediaId")
    suspend fun touchMediaCache(mediaId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM post_media")
    suspend fun getMediaCount(): Int

    @Transaction
    suspend fun updatePostMediaOrder(postId: Long, mediaIds: List<Long>) {
        mediaIds.forEachIndexed { index, mediaId ->
            updateMediaSortOrder(mediaId, index)
        }
    }
}