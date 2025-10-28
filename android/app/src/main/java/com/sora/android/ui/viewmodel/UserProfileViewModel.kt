package com.sora.android.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sora.android.data.local.TokenManager
import com.sora.android.data.remote.ApiService
import com.sora.android.data.remote.dto.CountryCollectionsResponse
import com.sora.android.domain.model.GlobeDataModel
import com.sora.android.domain.model.TravelStatsModel
import com.sora.android.domain.model.UserProfileModel
import com.sora.android.domain.repository.CountryRepository
import com.sora.android.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val countryRepository: CountryRepository,
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val userId: Long = savedStateHandle.get<Long>("userId") ?: 0L

    private val _currentUserId = MutableStateFlow<Long?>(null)
    val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfileModel?>(null)
    val userProfile: StateFlow<UserProfileModel?> = _userProfile.asStateFlow()

    private val _travelStats = MutableStateFlow<TravelStatsModel?>(null)
    val travelStats: StateFlow<TravelStatsModel?> = _travelStats.asStateFlow()

    private val _countryCollections = MutableStateFlow<CountryCollectionsResponse?>(null)
    val countryCollections: StateFlow<CountryCollectionsResponse?> = _countryCollections.asStateFlow()

    private val _globeData = MutableStateFlow<GlobeDataModel?>(null)
    val globeData: StateFlow<GlobeDataModel?> = _globeData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    private val _isFollowLoading = MutableStateFlow(false)
    val isFollowLoading: StateFlow<Boolean> = _isFollowLoading.asStateFlow()

    private var hasLocalFollowState = false

    init {
        loadCurrentUserId()
        loadProfile()
    }

    private fun loadCurrentUserId() {
        viewModelScope.launch {
            _currentUserId.value = tokenManager.getUserId()
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val profileResponse = apiService.getUserById(userId)
                if (profileResponse.isSuccessful && profileResponse.body() != null) {
                    val profile = profileResponse.body()!!
                    _userProfile.value = profile

                    if (!hasLocalFollowState) {
                        _isFollowing.value = profile.isFollowedByCurrentUser
                    }
                }

                userRepository.getUserTravelStats(userId).fold(
                    onSuccess = { stats ->
                        _travelStats.value = stats
                    },
                    onFailure = { exception ->
                        _error.value = exception.message
                    }
                )

                countryRepository.getUserCountryCollections(userId).firstOrNull()?.let { collections ->
                    _countryCollections.value = collections
                }

                val globeResponse = apiService.getProfileGlobeData(userId)
                if (globeResponse.isSuccessful) {
                    _globeData.value = globeResponse.body()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFollow() {
        viewModelScope.launch {
            _isFollowLoading.value = true
            hasLocalFollowState = true

            val wasFollowing = _isFollowing.value

            try {
                if (wasFollowing) {
                    _isFollowing.value = false
                    _travelStats.value?.let { stats ->
                        _travelStats.value = stats.copy(
                            totalFollowers = maxOf(0, stats.totalFollowers - 1)
                        )
                    }

                    userRepository.unfollowUser(userId).fold(
                        onSuccess = {
                            hasLocalFollowState = false
                        },
                        onFailure = { exception ->
                            _isFollowing.value = true
                            _travelStats.value?.let { stats ->
                                _travelStats.value = stats.copy(
                                    totalFollowers = stats.totalFollowers + 1
                                )
                            }
                            _error.value = exception.message
                            hasLocalFollowState = false
                        }
                    )
                } else {
                    _isFollowing.value = true
                    _travelStats.value?.let { stats ->
                        _travelStats.value = stats.copy(
                            totalFollowers = stats.totalFollowers + 1
                        )
                    }

                    userRepository.followUser(userId).fold(
                        onSuccess = {
                            hasLocalFollowState = false
                        },
                        onFailure = { exception ->
                            _isFollowing.value = false
                            _travelStats.value?.let { stats ->
                                _travelStats.value = stats.copy(
                                    totalFollowers = maxOf(0, stats.totalFollowers - 1)
                                )
                            }
                            _error.value = exception.message
                            hasLocalFollowState = false
                        }
                    )
                }
            } catch (e: Exception) {
                _isFollowing.value = wasFollowing
                _error.value = e.message
                hasLocalFollowState = false
            } finally {
                _isFollowLoading.value = false
            }
        }
    }

    fun refreshProfile() {
        loadProfile()
    }

    fun clearError() {
        _error.value = null
    }
}