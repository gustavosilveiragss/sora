package com.sora.android.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.sora.android.domain.model.CollectionCode
import com.sora.android.domain.model.CountryModel
import com.sora.android.domain.model.CountryVisitInfoModel
import com.sora.android.domain.model.PostModel
import com.sora.android.domain.model.UserModel
import com.sora.android.domain.repository.AuthRepository
import com.sora.android.domain.repository.CountryRepository
import com.sora.android.domain.repository.PostRepository
import com.sora.android.domain.repository.SocialRepository
import com.sora.android.ui.components.PostListFilters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CountryCollectionUiState(
    val country: CountryModel? = null,
    val user: UserModel? = null,
    val visitInfo: CountryVisitInfoModel? = null,
    val currentUserId: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val postsLoaded: Boolean = false
)

@HiltViewModel
class CountryCollectionViewModel @Inject constructor(
    private val countryRepository: CountryRepository,
    private val postRepository: PostRepository,
    private val socialRepository: SocialRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val userId: Long = checkNotNull(savedStateHandle.get<Long>("userId"))
    private val countryCode: String = checkNotNull(savedStateHandle.get<String>("countryCode"))
    private val initialTimeframe: String = savedStateHandle.get<String>("timeframe") ?: "month"
    private val initialSortBy: String = savedStateHandle.get<String>("sortBy") ?: "createdAt"

    private val _uiState = MutableStateFlow(CountryCollectionUiState())
    val uiState: StateFlow<CountryCollectionUiState> = _uiState.asStateFlow()

    private val _filters = MutableStateFlow(
        PostListFilters(
            timeframe = initialTimeframe,
            sortBy = initialSortBy
        )
    )
    val filters: StateFlow<PostListFilters> = _filters.asStateFlow()

    private val _likeModifications = MutableStateFlow<Map<Long, LikeModification>>(emptyMap())

    val posts: Flow<PagingData<PostModel>> = _filters.flatMapLatest { filters ->
        _uiState.update { it.copy(postsLoaded = false) }
        postRepository.getCountryPosts(
            userId = userId,
            countryCode = countryCode,
            collectionCode = filters.collectionCode?.name,
            cityName = filters.cityName,
            timeframe = filters.timeframe,
            sortBy = filters.sortBy,
            sortDirection = filters.sortDirection
        ).also {
            viewModelScope.launch {
                kotlinx.coroutines.delay(500)
                _uiState.update { it.copy(postsLoaded = true) }
            }
        }
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
        loadCountryInfo()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                _uiState.update { it.copy(currentUserId = user?.id) }
            }
        }
    }

    fun selectCollection(collection: CollectionCode?) {
        _filters.update { it.copy(collectionCode = collection) }
    }

    fun selectCity(city: String?) {
        _filters.update { it.copy(cityName = city) }
    }

    fun updateFilters(newFilters: PostListFilters) {
        _filters.value = newFilters
    }

    private fun loadCountryInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                countryRepository.getCountryPosts(
                    userId = userId,
                    countryCode = countryCode,
                    timeframe = initialTimeframe,
                    page = 0,
                    size = 1
                ).collect { response ->
                    _uiState.update {
                        it.copy(
                            country = response.country,
                            user = response.user,
                            visitInfo = response.visitInfo,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun retry() {
        loadCountryInfo()
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

            result.onFailure { error ->
                android.util.Log.e("CountryCollectionVM", "Error toggling like: ${error.message}")
                _likeModifications.update { modifications ->
                    modifications + (postId to LikeModification(currentlyLiked, currentLikesCount))
                }
            }
        }
    }

    fun likePost(postId: Long) {
        viewModelScope.launch {
            socialRepository.likePost(postId).onFailure { error ->
                android.util.Log.e("CountryCollectionVM", "Error liking post: ${error.message}")
            }
        }
    }

    fun unlikePost(postId: Long) {
        viewModelScope.launch {
            socialRepository.unlikePost(postId).onFailure { error ->
                android.util.Log.e("CountryCollectionVM", "Error unliking post: ${error.message}")
            }
        }
    }
}
