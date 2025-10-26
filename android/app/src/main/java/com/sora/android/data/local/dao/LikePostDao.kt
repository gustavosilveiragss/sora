package com.sora.android.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.sora.android.data.local.entity.LikePost
import com.sora.android.data.local.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface LikePostDao {

    @Query("SELECT * FROM like_post WHERE postId = :postId ORDER BY createdAt DESC")
    suspend fun getPostLikes(postId: Long): List<LikePost>

    @Query("SELECT u.* FROM user u INNER JOIN like_post l ON u.id = l.userId WHERE l.postId = :postId ORDER BY l.createdAt DESC")
    fun getPostLikeUsers(postId: Long): PagingSource<Int, User>

    @Query("SELECT * FROM like_post WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getUserLikes(userId: Long): List<LikePost>

    @Query("SELECT * FROM like_post WHERE userId = :userId AND postId = :postId")
    suspend fun getLike(userId: Long, postId: Long): LikePost?

    @Query("SELECT EXISTS(SELECT 1 FROM like_post WHERE userId = :userId AND postId = :postId)")
    suspend fun isPostLiked(userId: Long, postId: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM like_post WHERE userId = :userId AND postId = :postId)")
    fun isPostLikedFlow(userId: Long, postId: Long): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM like_post WHERE postId = :postId")
    suspend fun getPostLikesCount(postId: Long): Int

    @Query("SELECT COUNT(*) FROM like_post WHERE postId = :postId")
    fun getPostLikesCountFlow(postId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM like_post WHERE userId = :userId")
    suspend fun getUserLikesCount(userId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: LikePost)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLikes(likes: List<LikePost>)

    @Delete
    suspend fun deleteLike(like: LikePost)

    @Query("DELETE FROM like_post WHERE userId = :userId AND postId = :postId")
    suspend fun deleteLike(userId: Long, postId: Long)

    @Query("DELETE FROM like_post WHERE cacheTimestamp < :expiry")
    suspend fun deleteExpiredLikes(expiry: Long): Int

    @Query("UPDATE like_post SET cacheTimestamp = :timestamp WHERE id = :likeId")
    suspend fun touchLikeCache(likeId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM like_post")
    suspend fun getLikeCount(): Int
}