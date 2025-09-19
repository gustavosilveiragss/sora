package com.sora.android.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.sora.android.data.local.entity.Post
import com.sora.android.data.local.entity.DraftPost
import com.sora.android.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {

    @Query("SELECT * FROM post ORDER BY createdAt DESC")
    fun getAllPostsPaged(): PagingSource<Int, Post>

    @Query("SELECT * FROM post ORDER BY cacheTimestamp DESC LIMIT :limit")
    suspend fun getRecentPosts(limit: Int): List<Post>

    @Query("SELECT * FROM post WHERE id = :postId")
    suspend fun getPostById(postId: Long): Post?

    @Query("SELECT * FROM post WHERE id = :postId")
    fun getPostByIdFlow(postId: Long): Flow<Post?>

    @Query("SELECT * FROM post WHERE authorId = :authorId ORDER BY createdAt DESC")
    suspend fun getPostsByAuthor(authorId: Long): List<Post>

    @Query("SELECT * FROM post WHERE countryCode = :countryCode ORDER BY createdAt DESC")
    suspend fun getPostsByCountry(countryCode: String): List<Post>

    @Query("SELECT * FROM post WHERE countryCode = :countryCode AND collectionCode = :collectionCode ORDER BY createdAt DESC")
    suspend fun getPostsByCountryAndCollection(countryCode: String, collectionCode: String): List<Post>

    @Query("SELECT * FROM post WHERE sharedPostGroupId = :groupId ORDER BY createdAt DESC")
    suspend fun getPostsBySharedGroup(groupId: String): List<Post>

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

    @Query("SELECT * FROM draft_post ORDER BY createdAt DESC")
    suspend fun getAllDrafts(): List<DraftPost>

    @Query("SELECT * FROM draft_post WHERE syncStatus = :status ORDER BY createdAt ASC")
    suspend fun getDraftsByStatus(status: SyncStatus): List<DraftPost>

    @Query("SELECT * FROM draft_post WHERE syncStatus = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingDrafts(): List<DraftPost>

    @Query("SELECT * FROM draft_post WHERE localId = :localId")
    suspend fun getDraftById(localId: String): DraftPost?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: DraftPost)

    @Update
    suspend fun updateDraft(draft: DraftPost)

    @Delete
    suspend fun deleteDraft(draft: DraftPost)

    @Query("DELETE FROM draft_post WHERE localId = :localId")
    suspend fun deleteDraftById(localId: String)

    @Query("UPDATE draft_post SET syncStatus = :status WHERE localId = :localId")
    suspend fun updateDraftStatus(localId: String, status: SyncStatus)

    @Query("DELETE FROM draft_post WHERE createdAt < :expiry")
    suspend fun deleteExpiredDrafts(expiry: Long): Int

    @Query("SELECT COUNT(*) FROM draft_post")
    suspend fun getDraftCount(): Int
}