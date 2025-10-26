package com.sora.android.data.repository

import android.content.Context
import android.util.Log
import com.sora.android.data.local.TokenManager
import com.sora.android.data.remote.ApiService
import com.sora.android.data.remote.dto.CountryVisitedListResponse
import com.sora.android.data.remote.dto.RecentDestinationsResponse
import com.sora.android.data.remote.dto.toUserStatsModel
import com.sora.android.data.remote.dto.toLeaderboardModel
import com.sora.android.data.remote.dto.toRankingsModel
import com.sora.android.data.remote.util.NetworkUtils
import com.sora.android.domain.model.*
import com.sora.android.domain.repository.GamificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GamificationRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val userStatsDao: com.sora.android.data.local.dao.UserStatsDao,
    private val networkMonitor: com.sora.android.core.network.NetworkMonitor,
    @ApplicationContext private val context: Context
) : GamificationRepository {

    override suspend fun getUserStats(userId: Long): Flow<UserStatsModel> {
        return flow {
            val cachedStats = userStatsDao.getUserStats(userId)
            if (cachedStats != null && isCacheValid(cachedStats.cacheTimestamp)) {
                emit(cachedStats.toUserStatsModel())
            }

            if (!networkMonitor.isCurrentlyConnected()) {
                if (cachedStats != null) {
                    emit(cachedStats.toUserStatsModel())
                } else {
                    emit(createEmptyUserStats(userId))
                }
                return@flow
            }

            try {
                val response = apiService.getUserTravelStats(userId)
                if (response.isSuccessful) {
                    response.body()?.let { statsResponse ->
                        val userStats = statsResponse.toUserStatsModel()
                        val cachedEntity = userStats.toCachedEntity()
                        userStatsDao.insertUserStats(cachedEntity)
                        emit(userStats)
                    }
                } else if (cachedStats != null) {
                    emit(cachedStats.toUserStatsModel())
                } else {
                    emit(createEmptyUserStats(userId))
                }
            } catch (e: Exception) {
                if (cachedStats != null) {
                    emit(cachedStats.toUserStatsModel())
                } else {
                    emit(createEmptyUserStats(userId))
                }
            }
        }
    }

    override suspend fun getMyStats(): Flow<UserStatsModel> {
        return flow {
            try {
                val currentUserId = getCurrentUserId()
                Log.d("GamificationRepo", "Getting my stats for userId: $currentUserId")

                getUserStats(currentUserId).collect { stats ->
                    emit(stats)
                }
            } catch (e: Exception) {
                Log.e("GamificationRepo", "Exception getting my stats: ${e.message}", e)
                val currentUserId = getCurrentUserId()
                emit(createEmptyUserStats(currentUserId))
            }
        }
    }

    override suspend fun getLeaderboard(
        metric: String,
        timeframe: String,
        limit: Int
    ): Flow<LeaderboardModel> {
        return flow {
            try {
                Log.d("GamificationRepo", "Getting leaderboard: metric=$metric, timeframe=$timeframe")

                val response = apiService.getLeaderboard(metric, timeframe, limit)
                if (response.isSuccessful) {
                    response.body()?.let { leaderboardResponse ->
                        val leaderboard = leaderboardResponse.toLeaderboardModel()
                        emit(leaderboard)
                    } ?: run {
                        emit(createEmptyLeaderboard(metric, timeframe))
                    }
                } else {
                    Log.e("GamificationRepo", "Leaderboard API Error: ${response.code()}")
                    emit(createEmptyLeaderboard(metric, timeframe))
                }
            } catch (e: Exception) {
                Log.e("GamificationRepo", "Exception getting leaderboard: ${e.message}", e)
                emit(createEmptyLeaderboard(metric, timeframe))
            }
        }
    }

    override suspend fun getUserRankings(userId: Long): Flow<RankingsModel> {
        return flow {
            try {
                Log.d("GamificationRepo", "Getting user rankings for userId: $userId")

                val response = apiService.getUserRankings(userId)
                if (response.isSuccessful) {
                    response.body()?.let { rankingsResponse ->
                        val rankings = rankingsResponse.toRankingsModel()
                        emit(rankings)
                    } ?: run {
                        emit(RankingsModel())
                    }
                } else {
                    Log.e("GamificationRepo", "Rankings API Error: ${response.code()}")
                    emit(RankingsModel())
                }
            } catch (e: Exception) {
                Log.e("GamificationRepo", "Exception getting rankings: ${e.message}", e)
                emit(RankingsModel())
            }
        }
    }

    override suspend fun getCountriesVisited(userId: Long): Flow<CountryVisitedListResponse> {
        return flow {
            try {
                Log.d("GamificationRepo", "Getting countries visited for userId: $userId")

                val response = apiService.getCountriesVisited(userId)
                if (response.isSuccessful) {
                    response.body()?.let { countries ->
                        emit(countries)
                    }
                }
            } catch (e: Exception) {
                Log.e("GamificationRepo", "Exception getting countries: ${e.message}", e)
            }
        }
    }

    override suspend fun getRecentDestinations(
        userId: Long,
        limit: Int
    ): Flow<RecentDestinationsResponse> {
        return flow {
            try {
                Log.d("GamificationRepo", "Getting recent destinations for userId: $userId, limit: $limit")

                val response = apiService.getRecentDestinations(userId, limit)
                if (response.isSuccessful) {
                    response.body()?.let { destinations ->
                        emit(destinations)
                    }
                }
            } catch (e: Exception) {
                Log.e("GamificationRepo", "Exception getting recent destinations: ${e.message}", e)
            }
        }
    }

    override suspend fun getCachedUserStats(userId: Long): Flow<UserStatsModel?> {
        return flow {
            val cachedStats = userStatsDao.getUserStats(userId)
            emit(cachedStats?.toUserStatsModel())
        }
    }

    override suspend fun getCachedLeaderboard(metric: String): Flow<LeaderboardModel?> {
        return flow {
            // TODO: Implement local caching if needed
            emit(null)
        }
    }

    override suspend fun refreshUserStats(userId: Long): Result<Unit> {
        return try {
            val response = apiService.getUserTravelStats(userId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshLeaderboard(metric: String): Result<Unit> {
        return try {
            val response = apiService.getLeaderboard(metric, "all", 20)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun calculateTravelScore(userId: Long): Flow<Int> {
        return flow {
            try {
                getUserStats(userId).collect { stats ->
                    val score = (stats.travelStats.totalCountriesVisited * 10) +
                               (stats.travelStats.totalPostsCount * 5) +
                               (stats.travelStats.totalLikesReceived * 2)
                    emit(score)
                }
            } catch (e: Exception) {
                Log.e("GamificationRepo", "Error calculating travel score: ${e.message}", e)
                emit(0)
            }
        }
    }

    override suspend fun getUserPosition(userId: Long, metric: String): Flow<Int?> {
        return flow {
            try {
                getUserRankings(userId).collect { rankings ->
                    val position = when (metric) {
                        "countries" -> rankings.countriesRankAmongMutuals?.position
                        "posts" -> rankings.postsRankAmongMutuals?.position
                        else -> null
                    }
                    emit(position)
                }
            } catch (e: Exception) {
                Log.e("GamificationRepo", "Error getting user position: ${e.message}", e)
                emit(null)
            }
        }
    }

    override suspend fun getAchievements(userId: Long): Flow<List<AchievementModel>> {
        return flow {
            try {
                // For now, return empty list as achievements might be part of user stats later on
                emit(emptyList())
            } catch (e: Exception) {
                Log.e("GamificationRepo", "Error getting achievements: ${e.message}", e)
                emit(emptyList())
            }
        }
    }

    private suspend fun getCurrentUserId(): Long {
        return tokenManager.getUserId() ?: 1L
    }

    private fun createEmptyUserStats(userId: Long): UserStatsModel {
        return UserStatsModel(
            user = UserModel(
                id = userId,
                username = "unknown",
                firstName = "",
                lastName = "",
                profilePicture = null
            ),
            travelStats = TravelStatsModel(),
            rankings = null,
            achievements = emptyList()
        )
    }

    override suspend fun getFollowersLeaderboard(
        metric: String,
        limit: Int
    ): Result<LeaderboardModel> {
        return try {
            val response = apiService.getFollowersLeaderboard(metric, limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toLeaderboardModel())
            } else {
                Result.failure(Exception("Failed to load followers leaderboard"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFollowingLeaderboard(
        metric: String,
        limit: Int
    ): Result<LeaderboardModel> {
        return try {
            val response = apiService.getFollowingLeaderboard(metric, limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toLeaderboardModel())
            } else {
                Result.failure(Exception("Failed to load following leaderboard"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createEmptyLeaderboard(metric: String, timeframe: String): LeaderboardModel {
        return LeaderboardModel(
            metric = metric,
            timeframe = timeframe,
            entries = emptyList(),
            currentUserPosition = null
        )
    }

    private fun isCacheValid(timestamp: Long): Boolean {
        val cacheExpiryMs = 12 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - timestamp) < cacheExpiryMs
    }
}

private fun com.sora.android.data.local.entity.CachedUserStats.toUserStatsModel(): UserStatsModel {
    return UserStatsModel(
        user = UserModel(
            id = userId,
            username = username,
            firstName = "",
            lastName = "",
            profilePicture = null
        ),
        travelStats = TravelStatsModel(
            totalCountriesVisited = totalCountriesVisited,
            totalCitiesVisited = totalCitiesVisited,
            totalPostsCount = totalPostsCount,
            totalLikesReceived = totalLikesReceived,
            totalCommentsReceived = totalCommentsReceived,
            totalFollowers = totalFollowers,
            totalFollowing = totalFollowing
        ),
        rankings = null,
        achievements = emptyList()
    )
}

private fun UserStatsModel.toCachedEntity(): com.sora.android.data.local.entity.CachedUserStats {
    return com.sora.android.data.local.entity.CachedUserStats(
        userId = user.id,
        username = user.username,
        totalCountriesVisited = travelStats.totalCountriesVisited,
        totalCitiesVisited = travelStats.totalCitiesVisited,
        totalPostsCount = travelStats.totalPostsCount,
        totalLikesReceived = travelStats.totalLikesReceived,
        totalCommentsReceived = travelStats.totalCommentsReceived,
        totalFollowers = travelStats.totalFollowers,
        totalFollowing = travelStats.totalFollowing,
        cacheTimestamp = System.currentTimeMillis()
    )
}