package com.sora.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PostModel(
    val id: Long,
    val author: UserModel,
    val country: CountryModel,
    val collection: CollectionModel,
    val cityName: String,
    val caption: String? = null,
    val mediaUrls: List<String> = emptyList(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLikedByCurrentUser: Boolean = false,
    val createdAt: String,
    val updatedAt: String? = null,
    val visibilityType: VisibilityType = VisibilityType.PERSONAL,
    val sharedPostGroupId: String? = null
)

@Serializable
data class PostCreateRequest(
    val countryCode: String,
    val collectionCode: String,
    val cityName: String,
    val cityLatitude: Double,
    val cityLongitude: Double,
    val caption: String? = null,
    val sharingOption: SharingOption = SharingOption.PERSONAL_ONLY,
    val profileOwnerId: Long? = null,
    val visibilityOption: String? = null
)

@Serializable
data class PostUpdateRequest(
    val caption: String? = null,
    val collectionCode: String? = null
)

@Serializable
enum class VisibilityType {
    PERSONAL, SHARED
}

@Serializable
enum class SharingOption {
    PERSONAL_ONLY, PROFILE_ONLY, BOTH_PROFILES
}

@Serializable
data class MediaModel(
    val id: Long? = null,
    val fileName: String,
    val cloudinaryUrl: String,
    val mediaType: MediaType,
    val fileSize: Long? = null
)

@Serializable
enum class MediaType {
    IMAGE, VIDEO
}