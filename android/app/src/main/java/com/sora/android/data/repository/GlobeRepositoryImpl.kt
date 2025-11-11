package com.sora.android.data.repository

import com.sora.android.data.local.dao.GlobeDao
import com.sora.android.data.local.entity.ProfileCountryMarkerEntity
import com.sora.android.data.local.entity.ProfileGlobeDataEntity
import com.sora.android.data.remote.ApiService
import com.sora.android.domain.model.CountryMarkerModel
import com.sora.android.domain.model.GlobeDataModel
import com.sora.android.domain.model.GlobeType
import com.sora.android.domain.repository.GlobeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GlobeRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val dao: GlobeDao
) : GlobeRepository {

    override fun getProfileGlobeData(userId: Long): Flow<Result<GlobeDataModel>> = flow {
        try {
            val response = api.getProfileGlobeData(userId)
            if (response.isSuccessful && response.body() != null) {
                val globeData = response.body()!!
                android.util.Log.d("GlobeRepository", "API returned ${globeData.countryMarkers.size} countries")
                globeData.countryMarkers.forEach { marker ->
                    android.util.Log.d("GlobeRepository", "Country ${marker.countryCode} has ${marker.recentPosts.size} posts")
                }
                emit(Result.success(globeData))
            } else {
                emit(Result.failure(Exception(response.message())))
            }
        } catch (e: Exception) {
            android.util.Log.e("GlobeRepository", "API failed: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    override fun getMainGlobeData(): Flow<Result<GlobeDataModel>> = flow {
        try {
            val response = api.getMainGlobeData()
            if (response.isSuccessful && response.body() != null) {
                val globeData = response.body()!!
                android.util.Log.d("GlobeRepository", "Main globe: ${globeData.countryMarkers.size} countries")
                emit(Result.success(globeData))
            } else {
                emit(Result.failure(Exception(response.message())))
            }
        } catch (e: Exception) {
            android.util.Log.e("GlobeRepository", "Main globe API failed: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    override fun getExploreGlobeData(timeframe: String, minPosts: Int): Flow<Result<GlobeDataModel>> = flow {
        try {
            val response = api.getExploreGlobeData(timeframe, minPosts)
            if (response.isSuccessful && response.body() != null) {
                val globeData = response.body()!!
                android.util.Log.d("GlobeRepository", "Explore globe: ${globeData.countryMarkers.size} trending countries")
                emit(Result.success(globeData))
            } else {
                emit(Result.failure(Exception(response.message())))
            }
        } catch (e: Exception) {
            android.util.Log.e("GlobeRepository", "Explore globe API failed: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    private fun isCacheValid(timestamp: Long): Boolean {
        val cacheExpiryMs = 0L
        return false
    }
}

private fun ProfileGlobeDataEntity.toGlobeDataModel(markers: List<ProfileCountryMarkerEntity>): GlobeDataModel {
    return GlobeDataModel(
        globeType = GlobeType.PROFILE,
        totalCountriesWithActivity = totalCountriesWithActivity,
        totalRecentPosts = totalRecentPosts,
        lastUpdated = lastUpdated,
        countryMarkers = markers.map { it.toCountryMarkerModel() }
    )
}

private fun ProfileCountryMarkerEntity.toCountryMarkerModel(): CountryMarkerModel {
    return CountryMarkerModel(
        countryCode = countryCode,
        countryNameKey = countryNameKey,
        latitude = latitude,
        longitude = longitude,
        recentPostsCount = recentPostsCount,
        lastPostDate = lastPostDate,
        activeUsers = emptyList(),
        recentPosts = emptyList()
    )
}