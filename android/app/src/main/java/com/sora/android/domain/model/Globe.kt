package com.sora.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GlobeDataModel(
    val globeType: GlobeType,
    val totalCountriesWithActivity: Int,
    val totalRecentPosts: Int,
    val lastUpdated: String,
    val countryMarkers: List<CountryMarkerModel>
)

@Serializable
enum class GlobeType {
    MAIN, PROFILE, EXPLORE
}

@Serializable
data class CountryMarkerModel(
    val countryCode: String,
    val countryNameKey: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val recentPostsCount: Int,
    val lastPostDate: String? = null,
    val activeUsers: List<UserModel> = emptyList(),
    val recentPosts: List<GlobePostModel> = emptyList()
)

@Serializable
data class CountryRecentPostsModel(
    val country: CountryModel,
    val posts: List<PostModel>,
    val totalPosts: Int
)

@Serializable
data class GlobePostModel(
    val id: Long,
    val author: UserModel,
    val cityName: String,
    val thumbnailUrl: String? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLikedByCurrentUser: Boolean = false,
    val createdAt: String
)