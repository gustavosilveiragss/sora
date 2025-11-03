package com.sora.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationModel(
    val id: Long,
    val type: NotificationType,
    val triggerUser: UserSummaryModel? = null,
    val post: PostSummaryModel? = null,
    val commentPreview: String? = null,
    val isRead: Boolean = false,
    val createdAt: String
)

@Serializable
data class UserSummaryModel(
    val id: Long,
    val username: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val profilePicture: String? = null
)

@Serializable
data class PostSummaryModel(
    val id: Long,
    val author: UserSummaryModel,
    val cityName: String,
    val thumbnailUrl: String? = null,
    val likesCount: Int = 0,
    val createdAt: String
)

@Serializable
data class NotificationsResponseModel(
    val unreadCount: Long,
    val notifications: List<NotificationModel>,
    val currentPage: Int,
    val totalPages: Int,
    val totalElements: Long
)

@Serializable
data class UnreadCountModel(
    val count: Long
)

@Serializable
enum class NotificationType {
    LIKE,
    COMMENT,
    COMMENT_REPLY,
    FOLLOW
}