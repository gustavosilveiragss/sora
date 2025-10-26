package com.sora.android.domain.repository

import androidx.paging.PagingData
import com.sora.android.data.remote.dto.CommentCreateResponse
import com.sora.android.data.remote.dto.LikeCreateResponse
import com.sora.android.data.remote.dto.LikesCountResponse
import com.sora.android.domain.model.*
import kotlinx.coroutines.flow.Flow

interface SocialRepository {
    suspend fun followUser(userId: Long): Result<FollowModel>
    suspend fun unfollowUser(userId: Long): Result<Unit>

    suspend fun likePost(postId: Long): Result<LikeCreateResponse>
    suspend fun unlikePost(postId: Long): Result<Unit>
    fun getPostLikes(postId: Long): Flow<PagingData<UserModel>>
    suspend fun getLikesCount(postId: Long): Result<LikesCountResponse>

    suspend fun createComment(postId: Long, request: CommentCreateRequest): Result<CommentCreateResponse>
    suspend fun replyToComment(commentId: Long, request: CommentCreateRequest): Result<CommentCreateResponse>
    fun getPostComments(postId: Long): Flow<PagingData<CommentModel>>
    suspend fun updateComment(commentId: Long, request: CommentCreateRequest): Result<CommentModel>
    suspend fun deleteComment(commentId: Long): Result<Unit>
}