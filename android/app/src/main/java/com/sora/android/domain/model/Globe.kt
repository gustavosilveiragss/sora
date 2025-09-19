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
    val recentPostsCount: Int,
    val activeUsers: List<UserModel> = emptyList(),
    val recentPosts: List<PostModel> = emptyList()
)

@Serializable
data class CountryRecentPostsModel(
    val country: CountryModel,
    val posts: List<PostModel>,
    val totalPosts: Int
)