package com.sora.android.data.remote.dto

import com.sora.android.domain.model.*
import kotlinx.serialization.Serializable

@Serializable
data class UserGamificationStatsResponseDto(
    val user: UserStatsUserDto,
    val travelStats: TravelStatsDto,
    val rankings: RankingsDto? = null,
    val achievements: List<AchievementDto> = emptyList()
)

@Serializable
data class UserStatsUserDto(
    val id: Long,
    val username: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val profilePicture: String? = null
)

@Serializable
data class TravelStatsDto(
    val totalCountriesVisited: Int = 0,
    val totalCitiesVisited: Int = 0,
    val totalPostsCount: Int = 0,
    val totalLikesReceived: Int = 0,
    val totalCommentsReceived: Int = 0,
    val totalFollowers: Int = 0,
    val totalFollowing: Int = 0
)

@Serializable
data class RankingsDto(
    val countriesRankAmongMutuals: RankingDto? = null,
    val postsRankAmongMutuals: RankingDto? = null
)

@Serializable
data class RankingDto(
    val position: Int,
    val totalUsers: Int,
    val percentile: Double
)

@Serializable
data class AchievementDto(
    val id: Long? = null,
    val code: String = "",
    val nameKey: String = "",
    val descriptionKey: String = "",
    val iconName: String? = null,
    val unlockedAt: String = ""
)



@Serializable
data class LeaderboardResponseDto(
    val metric: String,
    val timeframe: String,
    val leaderboard: List<LeaderboardEntryDto>,
    val currentUserPosition: Int? = null
)

@Serializable
data class LeaderboardEntryDto(
    val position: Int,
    val user: UserStatsUserDto,
    val score: Int,
    val scoreName: String,
    val isCurrentUser: Boolean = false
)


fun UserGamificationStatsResponseDto.toUserStatsModel(): UserStatsModel {
    return UserStatsModel(
        user = UserModel(
            id = user.id,
            username = user.username,
            firstName = user.firstName ?: "",
            lastName = user.lastName ?: "",
            profilePicture = user.profilePicture
        ),
        travelStats = TravelStatsModel(
            totalCountriesVisited = travelStats.totalCountriesVisited,
            totalCitiesVisited = travelStats.totalCitiesVisited,
            totalPostsCount = travelStats.totalPostsCount,
            totalLikesReceived = travelStats.totalLikesReceived,
            totalCommentsReceived = travelStats.totalCommentsReceived,
            totalFollowers = travelStats.totalFollowers,
            totalFollowing = travelStats.totalFollowing,
            rankings = rankings?.toRankingsModel(),
            recentDestinations = emptyList()
        ),
        rankings = rankings?.toRankingsModel(),
        achievements = achievements.map { it.toAchievementModel() }
    )
}

fun RankingsDto.toRankingsModel(): RankingsModel {
    return RankingsModel(
        countriesRankAmongMutuals = countriesRankAmongMutuals?.toRankingModel(),
        postsRankAmongMutuals = postsRankAmongMutuals?.toRankingModel()
    )
}

fun RankingDto.toRankingModel(): RankingModel {
    return RankingModel(
        position = position,
        totalUsers = totalUsers,
        percentile = percentile
    )
}

fun AchievementDto.toAchievementModel(): AchievementModel {
    return AchievementModel(
        id = id ?: 0L,
        code = code,
        nameKey = nameKey,
        descriptionKey = descriptionKey,
        iconName = iconName,
        unlockedAt = unlockedAt
    )
}


fun LeaderboardResponseDto.toLeaderboardModel(): LeaderboardModel {
    return LeaderboardModel(
        metric = metric,
        timeframe = timeframe,
        entries = leaderboard.map { it.toLeaderboardEntryModel() },
        currentUserPosition = currentUserPosition
    )
}

fun LeaderboardEntryDto.toLeaderboardEntryModel(): LeaderboardEntryModel {
    return LeaderboardEntryModel(
        position = position,
        user = UserModel(
            id = user.id,
            username = user.username,
            firstName = user.firstName ?: "",
            lastName = user.lastName ?: "",
            profilePicture = user.profilePicture
        ),
        score = score,
        scoreName = scoreName,
        isCurrentUser = isCurrentUser
    )
}

fun RankingsDto.toUserRankingsModel(userId: Long): UserRankingsModel {
    return UserRankingsModel(
        userId = userId,
        countriesRankPosition = countriesRankAmongMutuals?.position,
        postsRankPosition = postsRankAmongMutuals?.position,
        likesRankPosition = null,
        totalUsers = countriesRankAmongMutuals?.totalUsers ?: postsRankAmongMutuals?.totalUsers ?: 0,
        percentileCountries = countriesRankAmongMutuals?.percentile,
        percentilePosts = postsRankAmongMutuals?.percentile,
        percentileLikes = null
    )
}


private fun parseLastVisitDate(dateString: String?): Long {
    return try {
        if (dateString.isNullOrBlank()) {
            System.currentTimeMillis()
        } else {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            formatter.parse(dateString)?.time ?: System.currentTimeMillis()
        }
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}