package com.sora.android.data.remote.dto

import com.sora.android.core.translation.TranslationManager
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
    val recentDestinations: List<RecentDestinationDto>
)

@Serializable
data class RecentDestinationDto(
    val country: CountryDto,
    val lastCityVisited: String,
    val lastPostDate: String,
    val recentPostsCount: Int
)

@Serializable
data class CountryDto(
    val id: Long,
    val code: String,
    val nameKey: String,
    val latitude: Double?,
    val longitude: Double?,
    val timezone: String?
)

fun RecentDestinationDto.toRecentDestinationModel(
    translationManager: TranslationManager
): RecentDestinationModel {
    return RecentDestinationModel(
        countryId = country.id,
        countryCode = country.code,
        countryName = translationManager.translateAny(country.nameKey, country.nameKey),
        countryNameKey = country.nameKey,
        cityName = lastCityVisited,
        firstVisitDate = null,
        lastVisitDate = com.sora.android.core.util.DateFormatter.formatApiDateToDisplay(lastPostDate),
        visitCount = 1,
        postsCount = recentPostsCount
    )
}

fun CountryDto.toCountryModel(): CountryModel {
    return CountryModel(
        id = id,
        code = code,
        nameKey = nameKey,
        latitude = latitude,
        longitude = longitude
    )
}

@Serializable
data class CommentCreateResponse(
    val message: String,
    val comment: CommentResponseDto
)

@Serializable
data class CommentResponseDto(
    val id: Long,
    val author: UserSummaryDto,
    val content: String,
    val repliesCount: Int,
    val replies: List<CommentResponseDto>,
    val isLikedByCurrentUser: Boolean? = false,
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class UserSummaryDto(
    val id: Long,
    val username: String,
    val firstName: String,
    val lastName: String,
    val bio: String? = null,
    val profilePicture: String? = null,
    val countriesVisitedCount: Int? = null,
    val isFollowedByCurrentUser: Boolean? = null
)

fun CommentResponseDto.toCommentModel(): CommentModel {
    return CommentModel(
        id = id,
        author = UserModel(
            id = author.id,
            username = author.username,
            firstName = author.firstName,
            lastName = author.lastName,
            profilePicture = author.profilePicture
        ),
        content = content,
        repliesCount = repliesCount,
        isLikedByCurrentUser = isLikedByCurrentUser ?: false,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

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