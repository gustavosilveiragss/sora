package com.sora.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FollowModel(
    val id: Long,
    val follower: UserModel,
    val following: UserModel,
    val createdAt: String
)

@Serializable
data class LikeModel(
    val id: Long,
    val user: UserModel,
    val post: PostModel? = null,
    val likedAt: String
)

@Serializable
data class CommentModel(
    val id: Long,
    val author: UserModel,
    val post: PostModel? = null,
    val parentComment: CommentModel? = null,
    val content: String,
    val repliesCount: Int = 0,
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class CommentCreateRequest(
    val content: String
)

@Serializable
data class TravelPermissionModel(
    val id: Long,
    val grantor: UserModel,
    val grantee: UserModel,
    val country: CountryModel,
    val status: PermissionStatus,
    val invitationMessage: String? = null,
    val createdAt: String,
    val respondedAt: String? = null
)

@Serializable
data class TravelPermissionRequest(
    val granteeUsername: String,
    val countryCode: String,
    val invitationMessage: String? = null
)

@Serializable
enum class PermissionStatus {
    PENDING, ACTIVE, DECLINED, REVOKED
}