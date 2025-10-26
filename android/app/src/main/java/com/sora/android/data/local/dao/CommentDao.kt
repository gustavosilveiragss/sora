package com.sora.android.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.sora.android.data.local.entity.Comment
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {

    @Query("SELECT * FROM comment WHERE postId = :postId AND parentCommentId IS NULL ORDER BY createdAt ASC")
    fun getPostComments(postId: Long): PagingSource<Int, Comment>

    @Query("SELECT * FROM comment WHERE postId = :postId AND parentCommentId IS NULL ORDER BY createdAt ASC")
    suspend fun getPostCommentsAsList(postId: Long): List<Comment>

    @Query("SELECT * FROM comment WHERE parentCommentId = :commentId ORDER BY createdAt ASC")
    suspend fun getCommentReplies(commentId: Long): List<Comment>

    @Query("SELECT * FROM comment WHERE parentCommentId = :commentId ORDER BY createdAt ASC")
    fun getCommentRepliesFlow(commentId: Long): Flow<List<Comment>>

    @Query("SELECT * FROM comment WHERE id = :commentId")
    suspend fun getCommentById(commentId: Long): Comment?

    @Query("SELECT * FROM comment WHERE id = :commentId")
    fun getCommentByIdFlow(commentId: Long): Flow<Comment?>

    @Query("SELECT * FROM comment WHERE authorId = :userId ORDER BY createdAt DESC")
    suspend fun getUserComments(userId: Long): List<Comment>

    @Query("SELECT COUNT(*) FROM comment WHERE postId = :postId")
    suspend fun getPostCommentsCount(postId: Long): Int

    @Query("SELECT COUNT(*) FROM comment WHERE postId = :postId")
    fun getPostCommentsCountFlow(postId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM comment WHERE parentCommentId = :commentId")
    suspend fun getCommentRepliesCount(commentId: Long): Int

    @Query("SELECT COUNT(*) FROM comment WHERE parentCommentId = :commentId")
    fun getCommentRepliesCountFlow(commentId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM comment WHERE authorId = :userId")
    suspend fun getUserCommentsCount(userId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: Comment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<Comment>)

    @Update
    suspend fun updateComment(comment: Comment)

    @Delete
    suspend fun deleteComment(comment: Comment)

    @Query("DELETE FROM comment WHERE id = :commentId")
    suspend fun deleteCommentById(commentId: Long)

    @Query("DELETE FROM comment WHERE cacheTimestamp < :expiry")
    suspend fun deleteExpiredComments(expiry: Long): Int

    @Query("UPDATE comment SET cacheTimestamp = :timestamp WHERE id = :commentId")
    suspend fun touchCommentCache(commentId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM comment")
    suspend fun getCommentCount(): Int

    @Transaction
    suspend fun insertCommentWithReplyCount(comment: Comment) {
        insertComment(comment)
        comment.parentCommentId?.let { parentId ->
            updateParentRepliesCount(parentId)
        }
    }

    @Query("UPDATE comment SET repliesCount = (SELECT COUNT(*) FROM comment c WHERE c.parentCommentId = :commentId) WHERE id = :commentId")
    suspend fun updateParentRepliesCount(commentId: Long)

    @Query("UPDATE comment SET isLikedByCurrentUser = :isLiked WHERE id = :commentId")
    suspend fun updateCommentLikeStatus(commentId: Long, isLiked: Boolean)
}