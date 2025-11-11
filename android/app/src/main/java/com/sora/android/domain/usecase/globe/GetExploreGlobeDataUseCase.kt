package com.sora.android.domain.usecase.globe

import com.sora.android.domain.model.GlobeDataModel
import com.sora.android.domain.repository.GlobeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetExploreGlobeDataUseCase @Inject constructor(
    private val globeRepository: GlobeRepository
) {
    operator fun invoke(
        timeframe: String = "week",
        minPosts: Int = 1
    ): Flow<Result<GlobeDataModel>> {
        return globeRepository.getExploreGlobeData(timeframe, minPosts)
            .map { result ->
                result.map { globeData ->
                    val maxPosts = globeData.countryMarkers.maxOfOrNull { it.recentPostsCount } ?: 1
                    globeData.copy(
                        countryMarkers = globeData.countryMarkers.map { marker ->
                            marker.copy(
                                intensity = calculateIntensity(marker.recentPostsCount, maxPosts)
                            )
                        }
                    )
                }
            }
    }

    private fun calculateIntensity(posts: Int, maxPosts: Int): Double {
        if (maxPosts == 0) return 0.0
        return (posts.toDouble() / maxPosts.toDouble()).coerceIn(0.0, 1.0)
    }
}
