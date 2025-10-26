package com.sora.android.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sora.android.R
import com.sora.android.domain.model.LeaderboardModel
import com.sora.android.domain.model.UserModel
import com.sora.android.domain.repository.GamificationRepository
import com.sora.android.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FollowingUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val following: List<UserModel> = emptyList(),
    val filteredFollowing: List<UserModel> = emptyList(),
    val searchQuery: String = "",
    val leaderboard: LeaderboardModel? = null,
    val isLoadingLeaderboard: Boolean = false,
    val selectedMetric: String = "countries"
)

@HiltViewModel
class FollowingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val gamificationRepository: GamificationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(FollowingUiState())
    val uiState: StateFlow<FollowingUiState> = _uiState.asStateFlow()

    private var currentUserId: Long = 0L
    private var isCurrentUser: Boolean = false

    fun loadFollowing(userId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            currentUserId = userId

            val currentUserProfile = userRepository.getCurrentUserProfile()
            isCurrentUser = currentUserProfile.getOrNull()?.id == userId

            userRepository.getUserFollowingList(userId, 0, 100).fold(
                onSuccess = { following ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        following = following,
                        filteredFollowing = following
                    )
                    if (isCurrentUser) {
                        loadLeaderboard(_uiState.value.selectedMetric)
                    }
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: context.getString(R.string.unknown_error)
                    )
                }
            )
        }
    }

    fun isCurrentUserProfile(): Boolean = isCurrentUser

    fun updateSearchQuery(query: String) {
        val filtered = if (query.isEmpty()) {
            _uiState.value.following
        } else {
            _uiState.value.following.filter { user ->
                user.username.contains(query, ignoreCase = true) ||
                user.firstName.contains(query, ignoreCase = true) ||
                user.lastName.contains(query, ignoreCase = true)
            }
        }

        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredFollowing = filtered
        )
    }

    fun loadLeaderboard(metric: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingLeaderboard = true,
                selectedMetric = metric
            )

            gamificationRepository.getFollowingLeaderboard(metric, 10).fold(
                onSuccess = { leaderboard ->
                    _uiState.value = _uiState.value.copy(
                        leaderboard = leaderboard,
                        isLoadingLeaderboard = false
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isLoadingLeaderboard = false
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}