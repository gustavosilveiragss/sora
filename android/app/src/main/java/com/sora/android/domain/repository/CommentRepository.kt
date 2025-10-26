package com.sora.android.domain.repository

import androidx.paging.PagingData
import com.sora.android.data.remote.dto.CommentCreateResponse
import com.sora.android.domain.model.*
import kotlinx.coroutines.flow.Flow

interface CommentRepository {
    suspend fun getPostComments(
        postId: Long,
        page: Int = 0,
        size: Int = 20
    ): Flow<PagingData<CommentModel>>

    suspend fun createComment(postId: Long, content: String): Result<CommentCreateResponse>
    suspend fun replyToComment(commentId: Long, content: String): Result<CommentCreateResponse>
    suspend fun updateComment(commentId: Long, content: String): Result<CommentModel>
    suspend fun deleteComment(commentId: Long): Result<Unit>

    suspend fun getCommentById(commentId: Long): Flow<CommentModel?>
    suspend fun getCommentReplies(
        commentId: Long,
        page: Int = 0,
        size: Int = 20
    ): Flow<PagingData<CommentModel>>

    suspend fun getCachedComments(postId: Long): Flow<List<CommentModel>>
    suspend fun getCachedCommentsAsList(postId: Long): List<CommentModel>
    suspend fun getCommentRepliesAsList(commentId: Long): List<CommentModel>
    suspend fun refreshPostComments(postId: Long): Result<Unit>

    suspend fun getCommentsCount(postId: Long): Flow<Int>
    suspend fun getRepliesCount(commentId: Long): Flow<Int>

    suspend fun likeComment(commentId: Long): Result<Unit>
    suspend fun unlikeComment(commentId: Long): Result<Unit>
}