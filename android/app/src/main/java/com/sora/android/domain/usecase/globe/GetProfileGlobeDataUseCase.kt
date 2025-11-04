package com.sora.android.domain.usecase.globe

import com.sora.android.domain.model.GlobeDataModel
import com.sora.android.domain.repository.GlobeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProfileGlobeDataUseCase @Inject constructor(
    private val globeRepository: GlobeRepository
) {
    operator fun invoke(userId: Long): Flow<Result<GlobeDataModel>> {
        return globeRepository.getProfileGlobeData(userId)
    }
}