package com.sora.android.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.sora.android.data.local.entity.Post
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {

    @Query("SELECT * FROM post ORDER BY createdAt DESC")
    fun getAllPostsPaged(): PagingSource<Int, Post>

    @Query("SELECT * FROM post WHERE profileOwnerId IN (SELECT followingId FROM follow WHERE followerId = :userId) OR authorId = :userId ORDER BY createdAt DESC")
    fun getFeedPosts(userId: Long): PagingSource<Int, Post>

    @Query("SELECT * FROM post ORDER BY cacheTimestamp DESC LIMIT :limit")
    suspend fun getRecentPosts(limit: Int): List<Post>

    @Query("SELECT * FROM post WHERE id = :postId")
    suspend fun getPostById(postId: Long): Post?

    @Query("SELECT * FROM post WHERE id = :postId")
    fun getPostByIdFlow(postId: Long): Flow<Post?>

    @Query("SELECT * FROM post WHERE authorId = :authorId ORDER BY createdAt DESC")
    fun getPostsByAuthor(authorId: Long): PagingSource<Int, Post>

    @Query("SELECT * FROM post WHERE profileOwnerId = :userId ORDER BY createdAt DESC")
    fun getPostsByProfileOwner(userId: Long): PagingSource<Int, Post>

    @Query("SELECT * FROM post WHERE countryCode = :countryCode AND profileOwnerId = :userId ORDER BY createdAt DESC")
    fun getCountryPosts(countryCode: String, userId: Long): PagingSource<Int, Post>

    @Query("SELECT * FROM post WHERE countryCode = :countryCode AND profileOwnerId = :userId AND collectionCode = :collectionCode ORDER BY createdAt DESC")
    fun getCountryCollectionPosts(countryCode: String, userId: Long, collectionCode: String): PagingSource<Int, Post>

    @Query("SELECT * FROM post WHERE countryCode = :countryCode AND profileOwnerId = :userId AND cityName = :cityName ORDER BY createdAt DESC")
    fun getCityPosts(countryCode: String, userId: Long, cityName: String): PagingSource<Int, Post>

    @Query("SELECT * FROM post WHERE sharedPostGroupId = :groupId ORDER BY createdAt DESC")
    suspend fun getPostsBySharedGroup(groupId: String): List<Post>

    @Query("SELECT COUNT(*) FROM post WHERE authorId = :userId")
    suspend fun getUserPostsCount(userId: Long): Int

    @Query("SELECT COUNT(*) FROM post WHERE profileOwnerId = :userId")
    suspend fun getProfilePostsCount(userId: Long): Int

    @Query("SELECT COUNT(*) FROM post WHERE countryCode = :countryCode AND profileOwnerId = :userId")
    suspend fun getCountryPostsCount(countryCode: String, userId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<Post>)

    @Update
    suspend fun updatePost(post: Post)

    @Delete
    suspend fun deletePost(post: Post)

    @Query("DELETE FROM post WHERE id = :postId")
    suspend fun deletePostById(postId: Long)

    @Query("UPDATE post SET likesCount = :likesCount, isLikedByCurrentUser = :isLiked WHERE id = :postId")
    suspend fun updatePostLikeStatus(postId: Long, likesCount: Int, isLiked: Boolean)

    @Query("UPDATE post SET commentsCount = :commentsCount WHERE id = :postId")
    suspend fun updatePostCommentsCount(postId: Long, commentsCount: Int)

    @Query("DELETE FROM post WHERE cacheTimestamp < :expiry")
    suspend fun deleteExpiredPosts(expiry: Long): Int

    @Query("DELETE FROM post WHERE id NOT IN (SELECT id FROM post ORDER BY cacheTimestamp DESC LIMIT :keepCount)")
    suspend fun deleteOldestPostsExceedingLimit(keepCount: Int)

    @Query("UPDATE post SET cacheTimestamp = :timestamp WHERE id = :postId")
    suspend fun touchPostCache(postId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM post")
    suspend fun getPostCount(): Int

    @Transaction
    suspend fun insertPostWithCleanup(post: Post, maxPosts: Int = 10000) {
        insertPost(post)
        val count = getPostCount()
        if (count > maxPosts) {
            deleteOldestPostsExceedingLimit(maxPosts)
        }
    }
}