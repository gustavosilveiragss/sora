package com.sora.android.domain.repository

import com.sora.android.data.remote.dto.CountryVisitedListResponse
import com.sora.android.data.remote.dto.RecentDestinationsResponse
import com.sora.android.domain.model.*
import kotlinx.coroutines.flow.Flow

interface GamificationRepository {
    suspend fun getUserStats(userId: Long): Flow<UserStatsModel>
    suspend fun getMyStats(): Flow<UserStatsModel>

    suspend fun getLeaderboard(
        metric: String = "countries",
        timeframe: String = "all",
        limit: Int = 20
    ): Flow<LeaderboardModel>

    suspend fun getUserRankings(userId: Long): Flow<RankingsModel>

    suspend fun getCountriesVisited(userId: Long): Flow<CountryVisitedListResponse>
    suspend fun getRecentDestinations(
        userId: Long,
        limit: Int = 10
    ): Flow<RecentDestinationsResponse>

    suspend fun getCachedUserStats(userId: Long): Flow<UserStatsModel?>
    suspend fun getCachedLeaderboard(metric: String): Flow<LeaderboardModel?>
    suspend fun refreshUserStats(userId: Long): Result<Unit>
    suspend fun refreshLeaderboard(metric: String): Result<Unit>

    suspend fun calculateTravelScore(userId: Long): Flow<Int>
    suspend fun getUserPosition(userId: Long, metric: String): Flow<Int?>
    suspend fun getAchievements(userId: Long): Flow<List<AchievementModel>>

    suspend fun getFollowersLeaderboard(metric: String, limit: Int): Result<LeaderboardModel>
    suspend fun getFollowingLeaderboard(metric: String, limit: Int): Result<LeaderboardModel>
}