package com.sora.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.sora.android.domain.model.PostModel
import com.sora.android.domain.repository.PostRepository
import com.sora.android.domain.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val socialRepository: SocialRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _likeModifications = MutableStateFlow<Map<Long, LikeModification>>(emptyMap())
    val likeModifications: StateFlow<Map<Long, LikeModification>> = _likeModifications.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    val feedPosts: Flow<PagingData<PostModel>> = _refreshTrigger.flatMapLatest {
        postRepository.getFeed(page = 0, size = 20)
    }.cachedIn(viewModelScope)

    fun refreshFeed() {
        _refreshTrigger.value++
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

    fun clearError() {
        _error.value = null
    }
}
