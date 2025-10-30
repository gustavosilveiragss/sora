package com.sora.android.data.repository

import android.content.Context
import androidx.paging.PagingData
import androidx.paging.map
import com.sora.android.data.local.TokenManager
import com.sora.android.data.local.dao.CommentDao
import com.sora.android.data.local.entity.Comment
import com.sora.android.data.remote.ApiService
import com.sora.android.data.remote.dto.CommentCreateResponse
import com.sora.android.data.remote.dto.toCommentModel
import com.sora.android.data.remote.util.NetworkUtils
import com.sora.android.domain.model.*
import com.sora.android.R
import com.sora.android.data.local.entity.User
import com.sora.android.data.remote.dto.CommentResponseDto
import com.sora.android.data.remote.dto.UserSummaryDto
import com.sora.android.domain.repository.CommentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val commentDao: CommentDao,
    private val userDao: com.sora.android.data.local.dao.UserDao,
    private val tokenManager: TokenManager,
    private val networkMonitor: com.sora.android.core.network.NetworkMonitor,
    @ApplicationContext private val context: Context
) : CommentRepository {

    override suspend fun getPostComments(
        postId: Long,
        page: Int,
        size: Int
    ): Flow<PagingData<CommentModel>> {
        return offlineFirstPaging(
            tag = "CommentRepository-$postId",
            networkMonitor = networkMonitor,
            getCached = {
                commentDao.getPostCommentsAsList(postId).map { it.toDomainModel() }
            },
            fetchFromApi = {
                val response = apiService.getPostComments(postId, page, size)
                if (response.isSuccessful) {
                    response.body()?.content?.also { commentDtos ->
                        val commentEntities = commentDtos.map { it.toCommentEntity(postId) }
                        val userEntities = commentDtos.map {
                            val existing = userDao.getUserById(it.author.id)
                            it.author.toUserEntity(existing)
                        }.distinctBy { it.id }
                        userDao.insertUsers(userEntities)
                        commentDao.insertComments(commentEntities)
                    }?.map { it.toCommentModel() }
                } else null
            }
        )
    }

    override suspend fun createComment(postId: Long, content: String): Result<CommentCreateResponse> {
        return try {
            val request = CommentCreateRequest(content)
            val response = apiService.createComment(postId, request)
            if (response.isSuccessful) {
                response.body()?.let { commentResponse ->
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

    override suspend fun replyToComment(commentId: Long, content: String): Result<CommentCreateResponse> {
        return try {
            val request = CommentCreateRequest(content)
            val response = apiService.replyToComment(commentId, request)
            if (response.isSuccessful) {
                response.body()?.let { commentResponse ->
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

    override suspend fun updateComment(commentId: Long, content: String): Result<CommentModel> {
        return try {
            val request = CommentCreateRequest(content)
            val response = apiService.updateComment(commentId, request)
            if (response.isSuccessful) {
                response.body()?.let { comment ->
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

    override suspend fun getCommentById(commentId: Long): Flow<CommentModel?> {
        return flow {
            val cachedComment = commentDao.getCommentById(commentId)
            if (cachedComment != null && isCacheValid(cachedComment.cacheTimestamp)) {
                emit(cachedComment.toDomainModel())
            } else {
                emit(cachedComment?.toDomainModel())
            }
        }
    }

    override suspend fun getCommentReplies(
        commentId: Long,
        page: Int,
        size: Int
    ): Flow<PagingData<CommentModel>> {
        return flow {
            android.util.Log.d("CommentRepository", "getCommentReplies: commentId=$commentId")

            val cachedReplies = commentDao.getCommentReplies(commentId)
                .map { it.toDomainModel() }

            android.util.Log.d("CommentRepository", "CACHE: Encontrado ${cachedReplies.size} replies")

            val isOffline = !networkMonitor.isCurrentlyConnected()

            if (isOffline) {
                android.util.Log.d("CommentRepository", "OFFLINE: Emitting cache and stopping")
                emit(PagingData.from(cachedReplies))
                return@flow
            }

            if (cachedReplies.isNotEmpty()) {
                android.util.Log.d("CommentRepository", "ONLINE + cache: Emitting cache first")
                emit(PagingData.from(cachedReplies))
            }

            try {
                val parentComment = commentDao.getCommentById(commentId)
                if (parentComment != null) {
                    val response = apiService.getPostComments(parentComment.postId, 0, 100)
                    if (response.isSuccessful) {
                        response.body()?.content?.let { commentDtos ->
                            val targetComment = commentDtos.find { it.id == commentId }
                            targetComment?.replies?.let { replyDtos ->
                                android.util.Log.d("CommentRepository", "SUCESSO DA API: ${replyDtos.size} replies, caching...")
                                val replies = replyDtos.map { it.toCommentModel() }
                                val replyEntities = replyDtos.map { it.toCommentEntity(parentComment.postId, commentId) }
                                val userEntities = replyDtos.map {
                                    val existing = userDao.getUserById(it.author.id)
                                    it.author.toUserEntity(existing)
                                }.distinctBy { it.id }

                                userDao.insertUsers(userEntities)
                                commentDao.insertComments(replyEntities)
                                emit(PagingData.from(replies))
                            }
                        }
                    } else if (cachedReplies.isEmpty()) {
                        android.util.Log.d("CommentRepository", "API FALHOU: ${response.code()}, emitindo empty")
                        emit(PagingData.empty())
                    }
                }
            } catch (e: Exception) {
                android.util.Log.d("CommentRepository", "EXCEPTION: ${e.message}")
                if (cachedReplies.isEmpty()) {
                    emit(PagingData.empty())
                }
            }
        }
    }

    override suspend fun getCachedComments(postId: Long): Flow<List<CommentModel>> {
        return flow {
            try {
                val cachedComments = commentDao.getPostCommentsAsList(postId)
                emit(cachedComments.map { it.toDomainModel() })
            } catch (e: Exception) {
                emit(emptyList())
            }
        }
    }

    override suspend fun getCachedCommentsAsList(postId: Long): List<CommentModel> {
        return try {
            commentDao.getPostCommentsAsList(postId).map { it.toDomainModel() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getCommentRepliesAsList(commentId: Long): List<CommentModel> {
        return try {
            commentDao.getCommentReplies(commentId).map { it.toDomainModel() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun refreshPostComments(postId: Long): Result<Unit> {
        return try {
            val response = apiService.getPostComments(postId, 0, 50)
            if (response.isSuccessful) {
                response.body()?.content?.let { commentDtos ->
                    val uniqueUsers = mutableMapOf<Long, com.sora.android.data.local.entity.User>()
                    val allCommentEntities = mutableListOf<Comment>()

                    commentDtos.forEach { commentDto ->
                        if (!uniqueUsers.containsKey(commentDto.author.id)) {
                            val existing = userDao.getUserById(commentDto.author.id)
                            uniqueUsers[commentDto.author.id] = commentDto.author.toUserEntity(existing)
                        }
                        allCommentEntities.add(commentDto.toCommentEntity(postId))

                        commentDto.replies.forEach { replyDto ->
                            if (!uniqueUsers.containsKey(replyDto.author.id)) {
                                val existing = userDao.getUserById(replyDto.author.id)
                                uniqueUsers[replyDto.author.id] = replyDto.author.toUserEntity(existing)
                            }
                            allCommentEntities.add(replyDto.toCommentEntity(postId, commentDto.id))
                        }
                    }

                    userDao.insertUsers(uniqueUsers.values.toList())
                    commentDao.insertComments(allCommentEntities)
                }
                Result.success(Unit)
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCommentsCount(postId: Long): Flow<Int> {
        return flow {
            try {
                val count = commentDao.getPostCommentsCount(postId)
                emit(count)
            } catch (e: Exception) {
                emit(0)
            }
        }
    }

    override suspend fun getRepliesCount(commentId: Long): Flow<Int> {
        return flow {
            try {
                val count = commentDao.getCommentRepliesCount(commentId)
                emit(count)
            } catch (e: Exception) {
                emit(0)
            }
        }
    }

    override suspend fun likeComment(commentId: Long): Result<Unit> {
        return try {
            android.util.Log.d("CommentRepository", "Chamando API para dar like no comentário: $commentId")
            val response = apiService.likeComment(commentId)
            android.util.Log.d("CommentRepository", "Resposta da API: code=${response.code()}, isSuccessful=${response.isSuccessful}")
            if (response.isSuccessful) {
                commentDao.updateCommentLikeStatus(commentId, true)
                android.util.Log.d("CommentRepository", "Like no comentário $commentId salvo localmente")
                Result.success(Unit)
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                android.util.Log.e("CommentRepository", "Erro ao dar like: $errorMessage (code=${response.code()})")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            android.util.Log.e("CommentRepository", "Exceção ao dar like no comentário: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun unlikeComment(commentId: Long): Result<Unit> {
        return try {
            android.util.Log.d("CommentRepository", "Chamando API para remover like do comentário: $commentId")
            val response = apiService.unlikeComment(commentId)
            android.util.Log.d("CommentRepository", "Resposta da API: code=${response.code()}, isSuccessful=${response.isSuccessful}")
            if (response.isSuccessful) {
                commentDao.updateCommentLikeStatus(commentId, false)
                android.util.Log.d("CommentRepository", "Unlike no comentário $commentId salvo localmente")
                Result.success(Unit)
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                android.util.Log.e("CommentRepository", "Erro ao remover like: $errorMessage (code=${response.code()})")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            android.util.Log.e("CommentRepository", "Exceção ao remover like do comentário: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun isCacheValid(timestamp: Long): Boolean {
        val cacheExpiryMs = 30 * 60 * 1000L
        return (System.currentTimeMillis() - timestamp) < cacheExpiryMs
    }
}

private fun CommentResponseDto.toCommentEntity(postId: Long, parentId: Long? = null): Comment {
    return Comment(
        id = id,
        postId = postId,
        authorId = author.id,
        authorUsername = author.username,
        authorProfilePicture = author.profilePicture,
        content = content,
        parentCommentId = parentId,
        repliesCount = repliesCount,
        isLikedByCurrentUser = isLikedByCurrentUser ?: false,
        createdAt = createdAt,
        updatedAt = updatedAt,
        cacheTimestamp = System.currentTimeMillis()
    )
}

private fun Comment.toDomainModel(): CommentModel {
    return CommentModel(
        id = id,
        author = UserModel(
            id = authorId,
            username = authorUsername,
            firstName = "",
            lastName = "",
            profilePicture = authorProfilePicture
        ),
        content = content,
        repliesCount = repliesCount,
        isLikedByCurrentUser = isLikedByCurrentUser,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private suspend fun UserSummaryDto.toUserEntity(existingUser: User? = null): User {
    return User(
        id = id,
        username = username,
        firstName = firstName,
        lastName = lastName,
        bio = bio ?: existingUser?.bio,
        profilePicture = profilePicture ?: existingUser?.profilePicture,
        cacheTimestamp = System.currentTimeMillis()
    )
}