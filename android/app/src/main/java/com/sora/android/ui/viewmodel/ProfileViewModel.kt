package com.sora.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.sora.android.data.remote.dto.CountryCollectionsResponse
import com.sora.android.data.remote.ApiService
import com.sora.android.domain.model.GlobeDataModel
import com.sora.android.domain.model.PostModel
import com.sora.android.domain.model.TravelStatsModel
import com.sora.android.domain.model.UserModel
import com.sora.android.domain.repository.CountryRepository
import com.sora.android.domain.repository.PostRepository
import com.sora.android.domain.repository.SocialRepository
import com.sora.android.domain.repository.UserRepository
import com.sora.android.domain.usecase.globe.GetProfileGlobeDataUseCase
import com.sora.android.data.repository.LocationRepositoryImpl
import com.mapbox.geojson.Point
import com.sora.android.ui.components.PostListFilters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val countryRepository: CountryRepository,
    private val postRepository: PostRepository,
    private val socialRepository: SocialRepository,
    private val getProfileGlobeDataUseCase: GetProfileGlobeDataUseCase,
    private val locationRepository: LocationRepositoryImpl,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val targetUserId: Long? = savedStateHandle.get<Long>("userId")

    private val _currentUserId = MutableStateFlow<Long?>(null)
    val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    private val _isOwnProfile = MutableStateFlow(targetUserId == null)
    val isOwnProfile: StateFlow<Boolean> = _isOwnProfile.asStateFlow()

    private val _userProfile = MutableStateFlow<UserModel?>(null)
    val userProfile: StateFlow<UserModel?> = _userProfile.asStateFlow()

    private val _travelStats = MutableStateFlow<TravelStatsModel?>(null)
    val travelStats: StateFlow<TravelStatsModel?> = _travelStats.asStateFlow()

    private val _countryCollections = MutableStateFlow<CountryCollectionsResponse?>(null)
    val countryCollections: StateFlow<CountryCollectionsResponse?> = _countryCollections.asStateFlow()

    private val _globeData = MutableStateFlow<GlobeDataModel?>(null)
    val globeData: StateFlow<GlobeDataModel?> = _globeData.asStateFlow()

    private val _userLocation = MutableStateFlow<Point?>(null)
    val userLocation: StateFlow<Point?> = _userLocation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _postFilters = MutableStateFlow(PostListFilters())
    val postFilters: StateFlow<PostListFilters> = _postFilters.asStateFlow()

    private val _likeModifications = MutableStateFlow<Map<Long, LikeModification>>(emptyMap())

    private val _postsLoaded = MutableStateFlow(false)
    val postsLoaded: StateFlow<Boolean> = _postsLoaded.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    private val _isFollowLoading = MutableStateFlow(false)
    val isFollowLoading: StateFlow<Boolean> = _isFollowLoading.asStateFlow()

    val userPosts: Flow<PagingData<PostModel>> = combine(
        userProfile,
        _postFilters
    ) { profile, filters ->
        profile to filters
    }.flatMapLatest { (profile, filters) ->
        _postsLoaded.update { false }
        profile?.id?.let { userId ->
            postRepository.getUserPosts(
                userId = userId,
                page = 0,
                size = 20
            ).also {
                viewModelScope.launch {
                    kotlinx.coroutines.delay(500)
                    _postsLoaded.update { true }
                }
            }
        } ?: kotlinx.coroutines.flow.flow { emit(PagingData.empty()) }
    }.combine(_likeModifications) { pagingData, modifications ->
        pagingData.map { post ->
            modifications[post.id]?.let { modification ->
                post.copy(
                    isLikedByCurrentUser = modification.isLiked,
                    likesCount = modification.likesCount
                )
            } ?: post
        }
    }.cachedIn(viewModelScope)

    init {
        loadCurrentUser()
        loadProfile()
        getCurrentUserLocation()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            userRepository.getCurrentUserProfile().fold(
                onSuccess = { profile ->
                    android.util.Log.d("SORA_USER", "Usuario atual carregado: ${profile.id}, bio=${profile.bio}")
                    _currentUserId.value = profile.id
                    if (targetUserId != null) {
                        _isOwnProfile.value = targetUserId == profile.id
                        android.util.Log.d("SORA_USER", "isOwnProfile atualizado: ${_isOwnProfile.value} (targetUserId=$targetUserId, currentUserId=${profile.id})")
                    }
                },
                onFailure = { }
            )
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                android.util.Log.d("SORA_USER", "Carregando perfil... targetUserId=$targetUserId")

                if (targetUserId == null) {
                    userRepository.getCurrentUserProfile().fold(
                        onSuccess = { profile ->
                            android.util.Log.d("SORA_USER", "Perfil proprio carregado: id=${profile.id}, username=${profile.username}, bio=${profile.bio}")
                            _userProfile.value = profile
                        },
                        onFailure = { exception ->
                            android.util.Log.e("SORA_USER", "Erro ao carregar perfil", exception)
                            _error.value = exception.message
                        }
                    )
                } else {
                    userRepository.getUserProfile(targetUserId).collect { profileModel ->
                        android.util.Log.d("SORA_USER", "Perfil de usuario carregado: ${profileModel?.id}")
                        profileModel?.let { profile ->
                            _userProfile.value = UserModel(
                                id = profile.id,
                                username = profile.username,
                                email = profile.email,
                                firstName = profile.firstName,
                                lastName = profile.lastName,
                                bio = profile.bio,
                                profilePicture = profile.profilePicture,
                                followersCount = profile.followersCount,
                                followingCount = profile.followingCount,
                                countriesVisitedCount = profile.countriesVisitedCount
                            )
                            _isFollowing.value = profile.isFollowedByCurrentUser
                        }
                    }
                }

                _userProfile.value?.id?.let { userId ->
                    android.util.Log.d("SORA_USER", "Carregando estatisticas de viagem para userId=$userId")
                    userRepository.getUserTravelStats(userId).fold(
                        onSuccess = { stats ->
                            android.util.Log.d("SORA_USER", "Estatisticas de viagem SUCESSO: countries=${stats.totalCountriesVisited}, posts=${stats.totalPostsCount}, followers=${stats.totalFollowers}")
                            _travelStats.value = stats
                            android.util.Log.d("SORA_USER", "Estatisticas de viagem definidas no estado: ${_travelStats.value}")
                        },
                        onFailure = { exception ->
                            android.util.Log.e("SORA_USER", "Estatisticas de viagem FALHARAM: ${exception.message}", exception)
                            _error.value = exception.message
                        }
                    )

                    if (targetUserId == null) {
                        countryRepository.getMyCountryCollections().collect { collections ->
                            _countryCollections.value = collections
                        }
                    } else {
                        countryRepository.getUserCountryCollections(targetUserId).collect { collections ->
                            _countryCollections.value = collections
                        }
                    }

                    getProfileGlobeDataUseCase(userId).onEach { result ->
                        result.onSuccess { globeData ->
                            android.util.Log.d("SORA_GLOBE", "Globe data loaded successfully: ${globeData.countryMarkers.size} countries")
                            _globeData.value = globeData
                        }.onFailure { exception ->
                            android.util.Log.e("SORA_GLOBE", "Failed to load globe data: ${exception.message}", exception)

                            val mockGlobeData = com.sora.android.domain.model.GlobeDataModel(
                                globeType = com.sora.android.domain.model.GlobeType.PROFILE,
                                totalCountriesWithActivity = 2,
                                totalRecentPosts = 5,
                                lastUpdated = "2025-11-04",
                                countryMarkers = listOf(
                                    com.sora.android.domain.model.CountryMarkerModel(
                                        countryCode = "BR",
                                        countryNameKey = "Brazil",
                                        latitude = -14.235,
                                        longitude = -51.9253,
                                        recentPostsCount = 3
                                    ),
                                    com.sora.android.domain.model.CountryMarkerModel(
                                        countryCode = "US",
                                        countryNameKey = "United States",
                                        latitude = 37.0902,
                                        longitude = -95.7129,
                                        recentPostsCount = 2
                                    )
                                )
                            )
                            android.util.Log.d("SORA_GLOBE", "Using mock globe data for testing")
                            _globeData.value = mockGlobeData
                        }
                    }.launchIn(viewModelScope)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshProfile() {
        loadProfile()
        getCurrentUserLocation()
    }

    private fun getCurrentUserLocation() {
        viewModelScope.launch {
            try {
                val location = locationRepository.getCurrentLocation()
                if (location != null) {
                    _userLocation.value = Point.fromLngLat(location.longitude, location.latitude)
                    android.util.Log.d("SORA_LOCATION", "User location obtained: ${location.latitude}, ${location.longitude}")
                } else {
                    android.util.Log.w("SORA_LOCATION", "Could not obtain user location, using default")
                    _userLocation.value = null
                }
            } catch (e: Exception) {
                android.util.Log.e("SORA_LOCATION", "Error getting location", e)
                _userLocation.value = null
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun updatePostFilters(filters: PostListFilters) {
        _postFilters.value = filters
    }

    fun toggleLike(postId: Long, currentlyLiked: Boolean, currentLikesCount: Int) {
        val newIsLiked = !currentlyLiked
        val newLikesCount = if (newIsLiked) currentLikesCount + 1 else maxOf(0, currentLikesCount - 1)

        _likeModifications.update { modifications ->
            modifications + (postId to LikeModification(newIsLiked, newLikesCount))
        }

        viewModelScope.launch {
            val result = if (newIsLiked) {
                socialRepository.likePost(postId)
            } else {
                socialRepository.unlikePost(postId)
            }

            result.onFailure {
                _likeModifications.update { modifications ->
                    modifications + (postId to LikeModification(currentlyLiked, currentLikesCount))
                }
            }
        }
    }

    fun toggleFollow() {
        val userId = targetUserId ?: return
        if (_isFollowLoading.value) return

        viewModelScope.launch {
            _isFollowLoading.value = true

            val result = if (_isFollowing.value) {
                socialRepository.unfollowUser(userId)
            } else {
                socialRepository.followUser(userId)
            }

            result.fold(
                onSuccess = {
                    _isFollowing.value = !_isFollowing.value
                    _userProfile.value?.let { profile ->
                        val newFollowersCount = if (_isFollowing.value) {
                            (profile.followersCount ?: 0) + 1
                        } else {
                            maxOf(0, (profile.followersCount ?: 0) - 1)
                        }
                        _userProfile.value = profile.copy(followersCount = newFollowersCount)
                    }
                },
                onFailure = { exception ->
                    android.util.Log.e("SORA_USER", "Erro ao alternar follow", exception)
                }
            )

            _isFollowLoading.value = false
        }
    }

    fun onMarkerSelected(marker: com.sora.android.domain.model.CountryMarkerModel) {
        android.util.Log.d("SORA_GLOBE", "Marker selected: ${marker.countryCode}")
    }
}