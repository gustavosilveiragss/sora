package com.sora.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationModel(
    val id: Long,
    val type: NotificationType,
    val message: String,
    val isRead: Boolean = false,
    val createdAt: String,
    val data: Map<String, String>? = null
)

@Serializable
enum class NotificationType {
    NEW_FOLLOWER,
    POST_LIKED,
    POST_COMMENTED,
    COMMENT_REPLIED,
    TRAVEL_PERMISSION_INVITATION,
    TRAVEL_PERMISSION_ACCEPTED,
    TRAVEL_PERMISSION_DECLINED,
    SHARED_POST_CREATED,
    ACHIEVEMENT_UNLOCKED,
    WEEKLY_DIGEST
}