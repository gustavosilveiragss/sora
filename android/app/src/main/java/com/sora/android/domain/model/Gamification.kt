package com.sora.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserStatsModel(
    val user: UserModel,
    val travelStats: TravelStatsModel,
    val rankings: RankingsModel? = null,
    val achievements: List<AchievementModel> = emptyList(),
    val continentStats: List<ContinentStatsModel> = emptyList()
)

@Serializable
data class TravelStatsModel(
    val totalCountriesVisited: Int = 0,
    val totalCitiesVisited: Int = 0,
    val totalPostsCount: Int = 0,
    val totalLikesReceived: Int = 0,
    val totalCommentsReceived: Int = 0,
    val totalFollowers: Int = 0
)

@Serializable
data class RankingsModel(
    val countriesRankAmongFollowed: RankingModel? = null,
    val postsRankAmongFollowed: RankingModel? = null
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
data class ContinentStatsModel(
    val continentCode: String,
    val continentNameKey: String,
    val countriesVisited: Int,
    val totalCountries: Int,
    val completionPercentage: Double
)

@Serializable
data class LeaderboardModel(
    val metric: String,
    val timeframe: String,
    val leaderboard: List<LeaderboardEntryModel>,
    val currentUserPosition: Int? = null
)

@Serializable
data class LeaderboardEntryModel(
    val position: Int,
    val user: UserModel,
    val score: Int
)