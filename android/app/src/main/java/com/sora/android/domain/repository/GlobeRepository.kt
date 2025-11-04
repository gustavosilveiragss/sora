package com.sora.android.domain.repository

import com.sora.android.domain.model.GlobeDataModel
import kotlinx.coroutines.flow.Flow

interface GlobeRepository {
    fun getProfileGlobeData(userId: Long): Flow<Result<GlobeDataModel>>
}