package com.sora.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.sora.android.data.repository.LocationRepositoryImpl
import com.sora.android.domain.model.GlobeDataModel
import com.sora.android.domain.repository.UserRepository
import com.sora.android.domain.usecase.globe.GetMainGlobeDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainFeedViewModel @Inject constructor(
    private val getMainGlobeDataUseCase: GetMainGlobeDataUseCase,
    private val locationRepository: LocationRepositoryImpl,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _globeData = MutableStateFlow<GlobeDataModel?>(null)
    val globeData: StateFlow<GlobeDataModel?> = _globeData.asStateFlow()

    private val _userLocation = MutableStateFlow<Point?>(null)
    val userLocation: StateFlow<Point?> = _userLocation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadUserLocation()
        loadGlobeData()
    }

    private fun loadUserLocation() {
        viewModelScope.launch {
            try {
                val location = locationRepository.getCurrentLocation()
                if (location != null) {
                    _userLocation.value = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude)
                }
            } catch (e: Exception) {
                _userLocation.value = null
            }
        }
    }

    fun loadGlobeData() {
        viewModelScope.launch {
            _isLoading.update { true }
            _error.update { null }

            getMainGlobeDataUseCase().collect { result ->
                result.fold(
                    onSuccess = { data ->
                        _globeData.update { data }
                        _isLoading.update { false }
                    },
                    onFailure = { exception ->
                        _error.update { exception.message }
                        _isLoading.update { false }
                    }
                )
            }
        }
    }

    fun refresh() {
        loadGlobeData()
    }
}
