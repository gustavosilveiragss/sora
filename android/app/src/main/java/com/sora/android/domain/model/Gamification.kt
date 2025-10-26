package com.sora.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserStatsModel(
    val user: UserModel,
    val travelStats: TravelStatsModel,
    val rankings: RankingsModel? = null,
    val achievements: List<AchievementModel> = emptyList()
)

@Serializable
data class TravelStatsModel(
    val totalCountriesVisited: Int = 0,
    val totalCitiesVisited: Int = 0,
    val totalPostsCount: Int = 0,
    val totalLikesReceived: Int = 0,
    val totalCommentsReceived: Int = 0,
    val totalFollowers: Int = 0,
    val totalFollowing: Int = 0,
    val rankings: RankingsModel? = null,
    val recentDestinations: List<RecentDestinationModel> = emptyList()
)

@Serializable
data class RankingsModel(
    val countriesRankAmongMutuals: RankingModel? = null,
    val postsRankAmongMutuals: RankingModel? = null
)

@Serializable
data class RecentDestinationModel(
    val countryId: Long? = null,
    val countryCode: String = "",
    val countryName: String = "",
    val countryNameKey: String = "",
    val cityName: String? = null,
    val firstVisitDate: String? = null,
    val lastVisitDate: String? = null,
    val visitCount: Int = 0,
    val postsCount: Int = 0
)

@Serializable
data class RankingModel(
    val position: Int,
    val totalUsers: Int,
    val percentile: Double
)

@Serializable
data class AchievementModel(
    val id: Long,
    val code: String,
    val nameKey: String,
    val descriptionKey: String,
    val iconName: String? = null,
    val unlockedAt: String
)


@Serializable
data class LeaderboardModel(
    val metric: String,
    val timeframe: String,
    val entries: List<LeaderboardEntryModel>,
    val currentUserPosition: Int? = null
)

@Serializable
data class LeaderboardEntryModel(
    val position: Int,
    val user: UserModel,
    val score: Int,
    val scoreName: String,
    val isCurrentUser: Boolean
)

@Serializable
data class UserRankingsModel(
    val userId: Long,
    val countriesRankPosition: Int?,
    val postsRankPosition: Int?,
    val likesRankPosition: Int?,
    val totalUsers: Int,
    val percentileCountries: Double?,
    val percentilePosts: Double?,
    val percentileLikes: Double?
)