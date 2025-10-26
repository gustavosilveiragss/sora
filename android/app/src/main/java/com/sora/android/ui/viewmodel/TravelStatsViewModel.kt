package com.sora.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sora.android.domain.repository.UserRepository
import com.sora.android.R
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


data class RecentDestination(
    val countryName: String,
    val cityName: String,
    val lastVisitDate: String,
    val postsCount: Int
)

data class TravelStatsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalCountriesVisited: Int = 0,
    val totalCitiesVisited: Int = 0,
    val totalPostsCount: Int = 0,
    val totalLikesReceived: Int = 0,
    val totalCommentsReceived: Int = 0,
    val totalFollowers: Int = 0,
    val countriesRankPosition: Int? = null,
    val postsRankPosition: Int? = null,
    val totalRankedUsers: Int = 0,
    val recentDestinations: List<RecentDestination> = emptyList()
)

@HiltViewModel
class TravelStatsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TravelStatsUiState())
    val uiState: StateFlow<TravelStatsUiState> = _uiState.asStateFlow()

    init {
        loadTravelStats()
    }

    fun loadTravelStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val currentUser = userRepository.getCurrentUserProfile().getOrThrow()

                val stats = userRepository.getUserTravelStats(currentUser.id).getOrThrow()
                val rankings = userRepository.getUserRankings(currentUser.id).getOrThrow()
                val recentDestinations = userRepository.getRecentDestinations(currentUser.id, 10).getOrThrow()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    totalCountriesVisited = stats.totalCountriesVisited,
                    totalCitiesVisited = stats.totalCitiesVisited,
                    totalPostsCount = stats.totalPostsCount,
                    totalLikesReceived = stats.totalLikesReceived,
                    totalCommentsReceived = stats.totalCommentsReceived,
                    totalFollowers = stats.totalFollowers,
                    countriesRankPosition = rankings.countriesRankPosition,
                    postsRankPosition = rankings.postsRankPosition,
                    totalRankedUsers = rankings.totalUsers,
                    recentDestinations = recentDestinations.map { dest ->
                        RecentDestination(
                            countryName = dest.countryName.ifEmpty { dest.countryCode },
                            cityName = dest.cityName ?: "",
                            lastVisitDate = dest.lastVisitDate ?: "N/A",
                            postsCount = dest.postsCount
                        )
                    }
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: context.getString(R.string.error_loading_stats)
                )
            }
        }
    }


    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun formatDate(timestamp: Long): String {
        return try {
            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            dateFormat.format(java.util.Date(timestamp))
        } catch (e: Exception) {
            context.getString(R.string.invalid_date)
        }
    }
}