package com.sora.android.domain.repository

import com.sora.android.domain.model.GlobeDataModel
import kotlinx.coroutines.flow.Flow

interface GlobeRepository {
    fun getProfileGlobeData(userId: Long): Flow<Result<GlobeDataModel>>
    fun getMainGlobeData(): Flow<Result<GlobeDataModel>>
    fun getExploreGlobeData(timeframe: String, minPosts: Int): Flow<Result<GlobeDataModel>>
}