package com.sora.android.data.repository

import android.content.Context
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.sora.android.data.local.TokenManager
import com.sora.android.data.local.dao.NotificationDao
import com.sora.android.data.local.entity.Notification
import com.sora.android.data.remote.ApiService
import com.sora.android.data.remote.dto.MarkAllReadResponse
import com.sora.android.data.remote.dto.NotificationMarkReadResponse
import com.sora.android.data.remote.util.NetworkUtils
import com.sora.android.domain.model.NotificationModel
import com.sora.android.domain.model.NotificationType
import com.sora.android.domain.model.PostSummaryModel
import com.sora.android.domain.model.UserSummaryModel
import com.sora.android.domain.repository.NotificationRepository
import com.sora.android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val notificationDao: NotificationDao,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : NotificationRepository {

    override suspend fun getNotifications(
        unreadOnly: Boolean,
        type: String?,
        page: Int,
        size: Int
    ): Flow<PagingData<NotificationModel>> {
        return Pager(
            config = PagingConfig(
                pageSize = size,
                enablePlaceholders = false,
                initialLoadSize = size
            ),
            pagingSourceFactory = {
                com.sora.android.data.paging.NotificationPagingSource(
                    apiService = apiService,
                    unreadOnly = unreadOnly,
                    type = type
                )
            }
        ).flow
    }

    override suspend fun markNotificationAsRead(notificationId: Long): Result<NotificationMarkReadResponse> {
        return try {
            Log.d("SORA_NOTIFICATION", "Marcando notificacao como lida: notificationId=$notificationId")

            val response = apiService.markNotificationAsRead(notificationId)
            if (response.isSuccessful) {
                response.body()?.let { markReadResponse ->
                    Log.d("SORA_NOTIFICATION", "Notificacao marcada como lida na API")

                    Log.d("SORA_NOTIFICATION", "ATUALIZANDO notificacao na DB local: notificationId=$notificationId")
                    notificationDao.markAsRead(notificationId)
                    Log.d("SORA_NOTIFICATION", "Notificacao atualizada com sucesso na DB local")

                    Result.success(markReadResponse)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Log.e("SORA_NOTIFICATION", "Marcar como lida falhou: ${response.code()} - $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("SORA_NOTIFICATION", "Excecao ao marcar notificacao como lida: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun markAllNotificationsAsRead(): Result<MarkAllReadResponse> {
        return try {
            Log.d("SORA_NOTIFICATION", "Marcando todas as notificacoes como lidas")

            val response = apiService.markAllNotificationsAsRead()
            if (response.isSuccessful) {
                response.body()?.let { markAllResponse ->
                    Log.d("SORA_NOTIFICATION", "Todas as notificacoes marcadas como lidas: count=${markAllResponse.markedCount}")

                    val userId = getCurrentUserId()
                    Log.d("SORA_NOTIFICATION", "ATUALIZANDO todas as notificacoes na DB local: userId=$userId")
                    val updatedCount = notificationDao.markAllAsRead(userId)
                    Log.d("SORA_NOTIFICATION", "Notificacoes atualizadas na DB local: count=$updatedCount")

                    Result.success(markAllResponse)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Log.e("SORA_NOTIFICATION", "Marcar todas como lidas falhou: ${response.code()} - $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("SORA_NOTIFICATION", "Excecao ao marcar todas como lidas: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getUnreadCount(): Flow<Int> {
        return flow {
            try {
                Log.d("SORA_NOTIFICATION", "Buscando contagem de nao lidas")

                val response = apiService.getUnreadNotificationsCount()
                if (response.isSuccessful) {
                    response.body()?.let { unreadCountResponse ->
                        Log.d("SORA_NOTIFICATION", "Contagem de nao lidas recebida: count=${unreadCountResponse.count}")
                        emit(unreadCountResponse.count.toInt())
                    }
                } else {
                    val userId = getCurrentUserId()
                    val cachedCount = notificationDao.getUnreadCount(userId)
                    Log.d("SORA_NOTIFICATION", "Usando contagem em cache: count=$cachedCount")
                    emit(cachedCount)
                }
            } catch (e: Exception) {
                Log.e("SORA_NOTIFICATION", "Excecao ao buscar contagem: ${e.message}", e)
                val userId = getCurrentUserId()
                val cachedCount = notificationDao.getUnreadCount(userId)
                emit(cachedCount)
            }
        }
    }

    override suspend fun getCachedNotifications(): Flow<List<NotificationModel>> {
        return flow {
            try {
                val userId = getCurrentUserId()
                Log.d("SORA_NOTIFICATION", "Buscando notificacoes em cache: userId=$userId")

                val pager = Pager(
                    config = PagingConfig(pageSize = 20),
                    pagingSourceFactory = { notificationDao.getUserNotifications(userId) }
                )

                emit(emptyList<NotificationModel>())
            } catch (e: Exception) {
                Log.e("SORA_NOTIFICATION", "Excecao ao buscar notificacoes em cache: ${e.message}", e)
                emit(emptyList())
            }
        }
    }

    override suspend fun getCachedUnreadCount(): Flow<Int> {
        return notificationDao.getUnreadCountFlow(getCurrentUserId())
    }

    override suspend fun refreshNotifications(): Result<Unit> {
        return try {
            Log.d("SORA_NOTIFICATION", "Atualizando notificacoes")

            val response = apiService.getNotifications(false, null, 0, 20)
            if (response.isSuccessful) {
                response.body()?.let { notificationsResponse ->
                    val notificationEntities = notificationsResponse.notifications.map { it.toEntity(getCurrentUserId()) }
                    notificationDao.insertNotifications(notificationEntities)
                    Log.d("SORA_NOTIFICATION", "Notificacoes atualizadas com sucesso")
                    Result.success(Unit)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("SORA_NOTIFICATION", "Excecao ao atualizar notificacoes: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun subscribeToNotifications(): Flow<NotificationModel> {
        return flow {
            Log.d("SORA_NOTIFICATION", "Subscribe to notifications not implemented yet")
        }
    }

    override suspend fun clearAllNotifications(): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            Log.d("SORA_NOTIFICATION", "DELETANDO todas as notificacoes: userId=$userId")
            notificationDao.deleteAllUserNotifications(userId)
            Log.d("SORA_NOTIFICATION", "Todas as notificacoes deletadas com sucesso")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SORA_NOTIFICATION", "Excecao ao deletar notificacoes: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getNotificationsByType(type: NotificationType): Flow<List<NotificationModel>> {
        return flow {
            try {
                val userId = getCurrentUserId()
                Log.d("SORA_NOTIFICATION", "Buscando notificacoes por tipo: type=$type, userId=$userId")

                val pager = Pager(
                    config = PagingConfig(pageSize = 20),
                    pagingSourceFactory = { notificationDao.getNotificationsByType(userId, type.name) }
                )

                emit(emptyList<NotificationModel>())
            } catch (e: Exception) {
                Log.e("SORA_NOTIFICATION", "Excecao ao buscar notificacoes por tipo: ${e.message}", e)
                emit(emptyList())
            }
        }
    }

    private suspend fun getCurrentUserId(): Long {
        return tokenManager.getUserId() ?: 1L
    }
}

private fun NotificationModel.toEntity(recipientId: Long): Notification {
    return Notification(
        id = id,
        recipientId = recipientId,
        type = type.name,
        triggerUserId = triggerUser?.id,
        triggerUserUsername = triggerUser?.username,
        triggerUserFirstName = triggerUser?.firstName,
        triggerUserLastName = triggerUser?.lastName,
        triggerUserProfilePicture = triggerUser?.profilePicture,
        postId = post?.id,
        postCityName = post?.cityName,
        postThumbnailUrl = post?.thumbnailUrl,
        commentPreview = commentPreview,
        isRead = isRead,
        createdAt = createdAt,
        cacheTimestamp = System.currentTimeMillis()
    )
}

private fun Notification.toModel(): NotificationModel {
    return NotificationModel(
        id = id,
        type = NotificationType.valueOf(type),
        triggerUser = if (triggerUserId != null) {
            UserSummaryModel(
                id = triggerUserId,
                username = triggerUserUsername ?: "",
                firstName = triggerUserFirstName,
                lastName = triggerUserLastName,
                profilePicture = triggerUserProfilePicture
            )
        } else null,
        post = if (postId != null) {
            PostSummaryModel(
                id = postId,
                author = UserSummaryModel(
                    id = triggerUserId ?: 0,
                    username = triggerUserUsername ?: "",
                    firstName = triggerUserFirstName,
                    lastName = triggerUserLastName,
                    profilePicture = triggerUserProfilePicture
                ),
                cityName = postCityName ?: "",
                thumbnailUrl = postThumbnailUrl,
                likesCount = 0,
                createdAt = createdAt
            )
        } else null,
        commentPreview = commentPreview,
        isRead = isRead,
        createdAt = createdAt
    )
}
