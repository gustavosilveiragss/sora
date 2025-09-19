package com.sora.android.data.remote.dto

import com.sora.android.domain.model.*
import kotlinx.serialization.Serializable

@Serializable
data class PagedResponse<T>(
    val content: List<T>,
    val size: Int,
    val number: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean,
    val numberOfElements: Int
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val timestamp: String? = null
)

@Serializable
data class ErrorResponse(
    val messageKey: String,
    val message: String
)

@Serializable
data class MediaUploadResponse(
    val media: List<MediaModel>,
    val message: String
)

@Serializable
data class LikeCreateResponse(
    val likesCount: Int,
    val like: LikeModel,
    val message: String
)

@Serializable
data class LikesCountResponse(
    val likesCount: Int
)

@Serializable
data class CountryCollectionsResponse(
    val userId: Long,
    val username: String,
    val totalCountriesVisited: Int,
    val totalCitiesVisited: Int,
    val totalPostsCount: Int,
    val countries: List<CountryCollectionModel>
)

@Serializable
data class CountryPostsResponse(
    val country: CountryModel,
    val user: UserModel,
    val visitInfo: CountryVisitInfoModel,
    val posts: PagedResponse<PostModel>
)

@Serializable
data class NotificationsResponse(
    val notifications: PagedResponse<NotificationModel>
)

@Serializable
data class NotificationMarkReadResponse(
    val notification: NotificationModel,
    val message: String
)

@Serializable
data class MarkAllReadResponse(
    val markedCount: Int,
    val message: String
)

@Serializable
data class UnreadCountResponse(
    val unreadCount: Int
)

@Serializable
data class CountryVisitedListResponse(
    val username: String,
    val userId: Long,
    val totalCountriesVisited: Int,
    val countries: List<CountryCollectionModel>
)

@Serializable
data class RecentDestinationsResponse(
    val username: String,
    val userId: Long,
    val recentDestinations: List<CountryCollectionModel>
)

@Serializable
data class CommentCreateResponse(
    val comment: com.sora.android.domain.model.CommentModel
)

@Serializable
data class UpdateProfileRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: String? = null,
    val username: String? = null
)

@Serializable
data class ProfilePictureResponse(
    val profilePictureUrl: String,
    val message: String
)