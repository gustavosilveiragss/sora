package com.sora.android.data.repository

import android.content.Context
import android.util.Log
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
            Log.d("SORA_USER", "Iniciando follow do usuario: userId=$userId")

            val response = apiService.followUser(userId)
            if (response.isSuccessful) {
                response.body()?.let { follow ->
                    Log.d("SORA_USER", "Follow bem-sucedido: follower=${follow.follower.username}, following=${follow.following.username}")

                    val followEntity = Follow(
                        id = generateFollowId(follow.follower.id, follow.following.id),
                        followerId = follow.follower.id,
                        followingId = follow.following.id,
                        createdAt = follow.createdAt ?: "",
                        cacheTimestamp = System.currentTimeMillis()
                    )
                    Log.d("SORA_USER", "INSERINDO follow na DB local: followerId=${followEntity.followerId}, followingId=${followEntity.followingId}")
                    followDao.insertFollow(followEntity)
                    Log.d("SORA_USER", "Follow inserido com sucesso na DB local")

                    Result.success(follow)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Log.e("SORA_USER", "Follow falhou: ${response.code()} - $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("SORA_USER", "Excecao ao fazer follow: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun generateFollowId(followerId: Long, followingId: Long): Long {
        return (followerId.toString() + followingId.toString()).hashCode().toLong()
    }

    override suspend fun unfollowUser(userId: Long): Result<Unit> {
        return try {
            Log.d("SORA_USER", "Iniciando unfollow do usuario: userId=$userId")

            val response = apiService.unfollowUser(userId)
            if (response.isSuccessful) {
                val currentUserId = getCurrentUserId()
                Log.d("SORA_USER", "Unfollow bem-sucedido, removendo da DB local")
                Log.d("SORA_USER", "DELETANDO follow da DB local: followerId=$currentUserId, followingId=$userId")
                followDao.deleteFollow(currentUserId, userId)
                Log.d("SORA_USER", "Follow deletado com sucesso da DB local")
                Result.success(Unit)
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Log.e("SORA_USER", "Unfollow falhou: ${response.code()} - $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("SORA_USER", "Excecao ao fazer unfollow: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun likePost(postId: Long): Result<LikeCreateResponse> {
        return try {
            Log.d("SORA_USER", "Iniciando like no post: postId=$postId")

            val response = apiService.likePost(postId)
            if (response.isSuccessful) {
                response.body()?.let { likeResponse ->
                    val currentUserId = getCurrentUserId()
                    Log.d("SORA_USER", "Like bem-sucedido: postId=$postId, likesCount=${likeResponse.likesCount}")

                    val likeEntity = LikePost(
                        id = likeResponse.like.id,
                        userId = currentUserId,
                        postId = postId,
                        createdAt = likeResponse.like.likedAt
                    )
                    Log.d("SORA_USER", "INSERINDO like na DB local: likeId=${likeEntity.id}, postId=$postId, userId=$currentUserId")
                    likePostDao.insertLike(likeEntity)
                    Log.d("SORA_USER", "Like inserido com sucesso na DB local")

                    Log.d("SORA_USER", "ATUALIZANDO status de like do post na DB local: postId=$postId, likesCount=${likeResponse.likesCount}")
                    postDao.updatePostLikeStatus(postId, likeResponse.likesCount, true)
                    Log.d("SORA_USER", "Status de like do post atualizado com sucesso")

                    Result.success(likeResponse)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Log.e("SORA_USER", "Like falhou: ${response.code()} - $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("SORA_USER", "Excecao ao dar like: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun unlikePost(postId: Long): Result<Unit> {
        return try {
            Log.d("SORA_USER", "Iniciando unlike no post: postId=$postId")

            val response = apiService.unlikePost(postId)
            if (response.isSuccessful) {
                val currentUserId = getCurrentUserId()
                Log.d("SORA_USER", "Unlike bem-sucedido, removendo da DB local")
                Log.d("SORA_USER", "DELETANDO like da DB local: postId=$postId, userId=$currentUserId")
                likePostDao.deleteLike(currentUserId, postId)
                Log.d("SORA_USER", "Like deletado com sucesso da DB local")

                val currentCount = postDao.getPostById(postId)?.likesCount ?: 0
                val newCount = maxOf(0, currentCount - 1)
                Log.d("SORA_USER", "ATUALIZANDO status de like do post: postId=$postId, novoCount=$newCount")
                postDao.updatePostLikeStatus(postId, newCount, false)
                Log.d("SORA_USER", "Status de like do post atualizado com sucesso")

                Result.success(Unit)
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Log.e("SORA_USER", "Unlike falhou: ${response.code()} - $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("SORA_USER", "Excecao ao remover like: ${e.message}", e)
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
            Log.d("SORA_USER", "Criando comentario no post: postId=$postId, conteudo=${request.content?.take(50)}")

            val response = apiService.createComment(postId, request)
            if (response.isSuccessful) {
                response.body()?.let { commentResponse ->
                    Log.d("SORA_USER", "Comentario criado com sucesso: commentId=${commentResponse.comment.id}, author=${commentResponse.comment.author.username}")

                    val commentEntity = Comment(
                        id = commentResponse.comment.id,
                        authorId = commentResponse.comment.author.id,
                        authorUsername = commentResponse.comment.author.username,
                        authorProfilePicture = commentResponse.comment.author.profilePicture,
                        postId = postId,
                        content = commentResponse.comment.content,
                        createdAt = commentResponse.comment.createdAt
                    )
                    Log.d("SORA_USER", "INSERINDO comentario na DB local: commentId=${commentEntity.id}, postId=$postId")
                    commentDao.insertComment(commentEntity)
                    Log.d("SORA_USER", "Comentario inserido com sucesso na DB local")

                    val currentCommentsCount = postDao.getPostById(postId)?.commentsCount ?: 0
                    Log.d("SORA_USER", "ATUALIZANDO contador de comentarios: postId=$postId, novoCount=${currentCommentsCount + 1}")
                    postDao.updatePostCommentsCount(postId, currentCommentsCount + 1)
                    Log.d("SORA_USER", "Contador de comentarios atualizado com sucesso")

                    Result.success(commentResponse)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Log.e("SORA_USER", "Criar comentario falhou: ${response.code()} - $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("SORA_USER", "Excecao ao criar comentario: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun replyToComment(commentId: Long, request: CommentCreateRequest): Result<CommentCreateResponse> {
        return try {
            Log.d("SORA_USER", "Respondendo comentario: commentId=$commentId, conteudo=${request.content?.take(50)}")

            val parentComment = commentDao.getCommentById(commentId)
            if (parentComment == null) {
                Log.e("SORA_USER", "Comentario pai nao encontrado: commentId=$commentId")
                return Result.failure(Exception(context.getString(R.string.error_parent_comment_not_found)))
            }

            val response = apiService.replyToComment(commentId, request)
            if (response.isSuccessful) {
                response.body()?.let { commentResponse ->
                    Log.d("SORA_USER", "Resposta criada com sucesso: replyId=${commentResponse.comment.id}, parentId=$commentId")

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
                    Log.d("SORA_USER", "INSERINDO resposta na DB local: replyId=${commentEntity.id}, parentId=$commentId")
                    commentDao.insertCommentWithReplyCount(commentEntity)
                    Log.d("SORA_USER", "Resposta inserida com sucesso na DB local")

                    Result.success(commentResponse)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Log.e("SORA_USER", "Responder comentario falhou: ${response.code()} - $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("SORA_USER", "Excecao ao responder comentario: ${e.message}", e)
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
            Log.d("SORA_USER", "Atualizando comentario: commentId=$commentId, novoConteudo=${request.content?.take(50)}")

            val existingComment = commentDao.getCommentById(commentId)
            if (existingComment == null) {
                Log.e("SORA_USER", "Comentario nao encontrado para atualizacao: commentId=$commentId")
                return Result.failure(Exception(context.getString(R.string.error_comment_not_found)))
            }

            val response = apiService.updateComment(commentId, request)
            if (response.isSuccessful) {
                response.body()?.let { comment ->
                    Log.d("SORA_USER", "Comentario atualizado com sucesso na API: commentId=$commentId")

                    val commentEntity = comment.toCommentEntity(existingComment.postId)
                    Log.d("SORA_USER", "ATUALIZANDO comentario na DB local: commentId=$commentId")
                    commentDao.updateComment(commentEntity)
                    Log.d("SORA_USER", "Comentario atualizado com sucesso na DB local")

                    Result.success(comment)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Log.e("SORA_USER", "Atualizar comentario falhou: ${response.code()} - $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("SORA_USER", "Excecao ao atualizar comentario: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteComment(commentId: Long): Result<Unit> {
        return try {
            Log.d("SORA_USER", "Deletando comentario: commentId=$commentId")

            val response = apiService.deleteComment(commentId)
            if (response.isSuccessful) {
                Log.d("SORA_USER", "Comentario deletado com sucesso na API, removendo da DB local")
                Log.d("SORA_USER", "DELETANDO comentario da DB local: commentId=$commentId")
                commentDao.deleteCommentById(commentId)
                Log.d("SORA_USER", "Comentario deletado com sucesso da DB local")
                Result.success(Unit)
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Log.e("SORA_USER", "Deletar comentario falhou: ${response.code()} - $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("SORA_USER", "Excecao ao deletar comentario: ${e.message}", e)
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