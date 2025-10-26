package com.sora.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sora.android.data.remote.dto.CountryCollectionsResponse
import com.sora.android.data.remote.ApiService
import com.sora.android.domain.model.GlobeDataModel
import com.sora.android.domain.model.TravelStatsModel
import com.sora.android.domain.model.UserModel
import com.sora.android.domain.repository.CountryRepository
import com.sora.android.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val countryRepository: CountryRepository,
    private val apiService: ApiService
) : ViewModel() {

    private val _userProfile = MutableStateFlow<UserModel?>(null)
    val userProfile: StateFlow<UserModel?> = _userProfile.asStateFlow()

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

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                userRepository.getCurrentUserProfile().fold(
                    onSuccess = { profile ->
                        _userProfile.value = profile
                    },
                    onFailure = { exception ->
                        _error.value = exception.message
                    }
                )

                _userProfile.value?.id?.let { userId ->
                    userRepository.getUserTravelStats(userId).fold(
                        onSuccess = { stats ->
                            _travelStats.value = stats
                        },
                        onFailure = { exception ->
                            _error.value = exception.message
                        }
                    )

                    countryRepository.getMyCountryCollections().collect { collections ->
                        _countryCollections.value = collections
                    }

                    val globeResponse = apiService.getProfileGlobeData(userId)
                    if (globeResponse.isSuccessful) {
                        _globeData.value = globeResponse.body()
                    }
                }
            } finally {
                _isLoading.value = false
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