package com.sora.android.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sora.android.R
import com.sora.android.data.local.TokenManager
import com.sora.android.data.remote.ApiService
import com.sora.android.data.repository.SocialRepositoryImpl
import com.sora.android.domain.model.UserSearchResultModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val apiService: ApiService,
    private val socialRepository: SocialRepositoryImpl,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    data class ExploreUiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val searchResults: List<UserSearchResultModel> = emptyList(),
        val searchQuery: String = "",
        val hasSearched: Boolean = false,
        val followingUserIds: Set<Long> = emptySet(),
        val currentUserId: Long? = null
    )

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        loadCurrentUserId()
        setupSearchDebounce()
    }

    private fun loadCurrentUserId() {
        viewModelScope.launch {
            val userId = tokenManager.getUserId()
            _uiState.value = _uiState.value.copy(currentUserId = userId)
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupSearchDebounce() {
        viewModelScope.launch {
            _searchQuery
                .debounce(1000L)
                .filter { it.trim().length >= 2 }
                .distinctUntilChanged()
                .collect { query ->
                    performSearch(query.trim())
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            error = if (query.trim().length in 1..1) {
                context.getString(R.string.min_search_length)
            } else null,
            hasSearched = query.isNotBlank()
        )

        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                searchResults = emptyList(),
                hasSearched = false,
                isLoading = false
            )
        } else if (query.trim().length >= 2) {
            _uiState.value = _uiState.value.copy(isLoading = true)
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val response = apiService.searchUsers(
                    query = query,
                    countryCode = null,
                    page = 0,
                    size = 50
                )

                if (response.isSuccessful) {
                    val results = response.body()?.content ?: emptyList()
                    loadFollowingStatus(results.map { it.id })

                    _uiState.value = _uiState.value.copy(
                        searchResults = results,
                        isLoading = false,
                        hasSearched = true,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = context.getString(R.string.search_error),
                        searchResults = emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = context.getString(R.string.search_error),
                    searchResults = emptyList()
                )
            }
        }
    }

    private fun loadFollowingStatus(userIds: List<Long>) {
        viewModelScope.launch {
            try {
                val followingIds = mutableSetOf<Long>()
                userIds.forEach { userId ->
                    val isFollowing = checkIfFollowing(userId)
                    if (isFollowing) {
                        followingIds.add(userId)
                    }
                }
                _uiState.value = _uiState.value.copy(followingUserIds = followingIds)
            } catch (e: Exception) {
            }
        }
    }

    private suspend fun checkIfFollowing(userId: Long): Boolean {
        return try {
            false
        } catch (e: Exception) {
            false
        }
    }

    fun toggleFollow(userId: Long, isCurrentlyFollowing: Boolean) {
        viewModelScope.launch {
            try {
                val updatedFollowing = _uiState.value.followingUserIds.toMutableSet()

                if (isCurrentlyFollowing) {
                    updatedFollowing.remove(userId)
                    _uiState.value = _uiState.value.copy(followingUserIds = updatedFollowing)
                    socialRepository.unfollowUser(userId)
                } else {
                    updatedFollowing.add(userId)
                    _uiState.value = _uiState.value.copy(followingUserIds = updatedFollowing)
                    socialRepository.followUser(userId)
                }
            } catch (e: Exception) {
                val updatedFollowing = _uiState.value.followingUserIds.toMutableSet()
                if (isCurrentlyFollowing) {
                    updatedFollowing.add(userId)
                } else {
                    updatedFollowing.remove(userId)
                }
                _uiState.value = _uiState.value.copy(followingUserIds = updatedFollowing)
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _uiState.value = ExploreUiState()
    }

    fun retry() {
        if (_uiState.value.searchQuery.trim().length >= 2) {
            performSearch(_uiState.value.searchQuery.trim())
        }
    }
}
