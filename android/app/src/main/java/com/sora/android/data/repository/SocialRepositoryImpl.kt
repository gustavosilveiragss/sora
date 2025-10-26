package com.sora.android.data.repository

import android.content.Context
import androidx.paging.PagingData
import com.sora.android.data.local.TokenManager
import com.sora.android.data.local.dao.*
import com.sora.android.data.local.entity.*
import com.sora.android.data.remote.ApiService
import com.sora.android.data.remote.dto.CommentCreateResponse
import com.sora.android.data.remote.dto.LikeCreateResponse
import com.sora.android.data.remote.dto.LikesCountResponse
import com.sora.android.data.remote.dto.toCommentModel
import com.sora.android.data.remote.util.NetworkUtils
import com.sora.android.domain.model.*
import com.sora.android.domain.repository.SocialRepository
import com.sora.android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val followDao: FollowDao,
    private val likePostDao: LikePostDao,
    private val commentDao: CommentDao,
    private val userDao: UserDao,
    private val postDao: PostDao,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : SocialRepository {

    override suspend fun followUser(userId: Long): Result<FollowModel> {
        return try {
            val response = apiService.followUser(userId)
            if (response.isSuccessful) {
                response.body()?.let { follow ->
                    val followEntity = Follow(
                        id = follow.id,
                        followerId = follow.follower.id,
                        followingId = follow.following.id,
                        createdAt = follow.createdAt ?: ""
                    )
                    followDao.insertFollow(followEntity)
                    Result.success(follow)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unfollowUser(userId: Long): Result<Unit> {
        return try {
            val response = apiService.unfollowUser(userId)
            if (response.isSuccessful) {
                val currentUserId = getCurrentUserId()
                followDao.deleteFollow(currentUserId, userId)
                Result.success(Unit)
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun likePost(postId: Long): Result<LikeCreateResponse> {
        return try {
            val response = apiService.likePost(postId)
            if (response.isSuccessful) {
                response.body()?.let { likeResponse ->
                    val currentUserId = getCurrentUserId()
                    val likeEntity = LikePost(
                        id = likeResponse.like.id,
                        userId = currentUserId,
                        postId = postId,
                        createdAt = likeResponse.like.likedAt
                    )
                    likePostDao.insertLike(likeEntity)

                    postDao.updatePostLikeStatus(postId, likeResponse.likesCount, true)

                    Result.success(likeResponse)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unlikePost(postId: Long): Result<Unit> {
        return try {
            val response = apiService.unlikePost(postId)
            if (response.isSuccessful) {
                val currentUserId = getCurrentUserId()
                likePostDao.deleteLike(currentUserId, postId)

                val currentCount = postDao.getPostById(postId)?.likesCount ?: 0
                val newCount = maxOf(0, currentCount - 1)
                postDao.updatePostLikeStatus(postId, newCount, false)

                Result.success(Unit)
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getPostLikes(postId: Long): Flow<PagingData<UserModel>> {
        return flow {
            try {
                val response = apiService.getPostLikes(postId, 0, 20)
                if (response.isSuccessful) {
                    response.body()?.content?.let { likes ->
                        val users = likes.map { like ->
                            UserModel(
                                id = like.user.id,
                                username = like.user.username,
                                firstName = like.user.firstName,
                                lastName = like.user.lastName,
                                profilePicture = like.user.profilePicture
                            )
                        }
                        emit(PagingData.from(users))
                    }
                } else {
                    emit(PagingData.empty())
                }
            } catch (e: Exception) {
                emit(PagingData.empty())
            }
        }
    }

    override suspend fun getLikesCount(postId: Long): Result<LikesCountResponse> {
        return try {
            val response = apiService.getPostLikesCount(postId)
            if (response.isSuccessful) {
                response.body()?.let { likesCount ->
                    Result.success(likesCount)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val cachedCount = likePostDao.getPostLikesCount(postId)
                Result.success(LikesCountResponse(cachedCount))
            }
        } catch (e: Exception) {
            val cachedCount = likePostDao.getPostLikesCount(postId)
            Result.success(LikesCountResponse(cachedCount))
        }
    }

    override suspend fun createComment(postId: Long, request: CommentCreateRequest): Result<CommentCreateResponse> {
        return try {
            val response = apiService.createComment(postId, request)
            if (response.isSuccessful) {
                response.body()?.let { commentResponse ->
                    val commentEntity = Comment(
                        id = commentResponse.comment.id,
                        authorId = commentResponse.comment.author.id,
                        authorUsername = commentResponse.comment.author.username,
                        authorProfilePicture = commentResponse.comment.author.profilePicture,
                        postId = postId,
                        content = commentResponse.comment.content,
                        createdAt = commentResponse.comment.createdAt
                    )
                    commentDao.insertComment(commentEntity)

                    val currentCommentsCount = postDao.getPostById(postId)?.commentsCount ?: 0
                    postDao.updatePostCommentsCount(postId, currentCommentsCount + 1)

                    Result.success(commentResponse)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun replyToComment(commentId: Long, request: CommentCreateRequest): Result<CommentCreateResponse> {
        return try {
            val parentComment = commentDao.getCommentById(commentId)
            if (parentComment == null) {
                return Result.failure(Exception(context.getString(R.string.error_parent_comment_not_found)))
            }

            val response = apiService.replyToComment(commentId, request)
            if (response.isSuccessful) {
                response.body()?.let { commentResponse ->
                    val commentEntity = Comment(
                        id = commentResponse.comment.id,
                        authorId = commentResponse.comment.author.id,
                        authorUsername = commentResponse.comment.author.username,
                        authorProfilePicture = commentResponse.comment.author.profilePicture,
                        postId = parentComment.postId,
                        parentCommentId = commentId,
                        content = commentResponse.comment.content,
                        createdAt = commentResponse.comment.createdAt
                    )
                    commentDao.insertCommentWithReplyCount(commentEntity)
                    Result.success(commentResponse)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getPostComments(postId: Long): Flow<PagingData<CommentModel>> {
        return flow {
            try {
                val response = apiService.getPostComments(postId, 0, 20)
                if (response.isSuccessful) {
                    response.body()?.content?.let { commentDtos ->
                        val comments = commentDtos.map { it.toCommentModel() }
                        emit(PagingData.from(comments))
                    }
                } else {
                    emit(PagingData.empty())
                }
            } catch (e: Exception) {
                emit(PagingData.empty())
            }
        }
    }

    override suspend fun updateComment(commentId: Long, request: CommentCreateRequest): Result<CommentModel> {
        return try {
            val existingComment = commentDao.getCommentById(commentId)
            if (existingComment == null) {
                return Result.failure(Exception(context.getString(R.string.error_comment_not_found)))
            }

            val response = apiService.updateComment(commentId, request)
            if (response.isSuccessful) {
                response.body()?.let { comment ->
                    val commentEntity = comment.toCommentEntity(existingComment.postId)
                    commentDao.updateComment(commentEntity)
                    Result.success(comment)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteComment(commentId: Long): Result<Unit> {
        return try {
            val response = apiService.deleteComment(commentId)
            if (response.isSuccessful) {
                commentDao.deleteCommentById(commentId)
                Result.success(Unit)
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getCurrentUserId(): Long {
        return tokenManager.getUserId() ?: 1L
    }
}

private fun Comment.toCommentModel(): CommentModel {
    return CommentModel(
        id = id,
        author = UserModel(authorId, authorUsername, "", "", authorProfilePicture ?: ""),
        content = content,
        repliesCount = repliesCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun CommentModel.toCommentEntity(postId: Long): Comment {
    return Comment(
        id = id,
        authorId = author.id,
        authorUsername = author.username,
        authorProfilePicture = author.profilePicture,
        postId = postId,
        content = content,
        repliesCount = repliesCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}