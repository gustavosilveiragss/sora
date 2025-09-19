package com.sora.android.domain.repository

import androidx.paging.PagingData
import com.sora.android.data.remote.dto.MarkAllReadResponse
import com.sora.android.data.remote.dto.NotificationMarkReadResponse
import com.sora.android.domain.model.*
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    suspend fun getNotifications(
        unreadOnly: Boolean = false,
        type: String? = null,
        page: Int = 0,
        size: Int = 20
    ): Flow<PagingData<NotificationModel>>

    suspend fun markNotificationAsRead(notificationId: Long): Result<NotificationMarkReadResponse>
    suspend fun markAllNotificationsAsRead(): Result<MarkAllReadResponse>
    suspend fun getUnreadCount(): Flow<Int>

    suspend fun getCachedNotifications(): Flow<List<NotificationModel>>
    suspend fun getCachedUnreadCount(): Flow<Int>
    suspend fun refreshNotifications(): Result<Unit>

    suspend fun subscribeToNotifications(): Flow<NotificationModel>
    suspend fun clearAllNotifications(): Result<Unit>

    suspend fun getNotificationsByType(type: NotificationType): Flow<List<NotificationModel>>
}