package com.sora.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sora.android.data.remote.dto.CommentResponseDto
import com.sora.android.domain.model.CommentModel
import com.sora.android.domain.repository.CommentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommentViewModel @Inject constructor(
    private val commentRepository: CommentRepository
) : ViewModel() {

    private val _comments = MutableStateFlow<List<CommentWithReplies>>(emptyList())
    val comments: StateFlow<List<CommentWithReplies>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    private val _currentUserId = MutableStateFlow<Long?>(null)

    fun setCurrentUserId(userId: Long) {
        _currentUserId.value = userId
    }

    fun loadComments(postId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val cachedFirst = commentRepository.getCachedCommentsAsList(postId)
                if (cachedFirst.isNotEmpty()) {
                    _comments.value = cachedFirst.map { comment ->
                        CommentWithReplies(
                            comment = comment,
                            replies = emptyList(),
                            isExpanded = false
                        )
                    }
                    _isLoading.value = false
                }

                commentRepository.refreshPostComments(postId).fold(
                    onSuccess = {
                        val refreshedComments = commentRepository.getCachedCommentsAsList(postId)
                        _comments.value = refreshedComments.map { comment ->
                            CommentWithReplies(
                                comment = comment,
                                replies = emptyList(),
                                isExpanded = false
                            )
                        }
                    },
                    onFailure = { exception ->
                        if (cachedFirst.isEmpty()) {
                            _error.value = exception.message
                        }
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun postComment(postId: Long, content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            _isPosting.value = true
            _error.value = null

            commentRepository.createComment(postId, content).fold(
                onSuccess = {
                    loadComments(postId)
                },
                onFailure = { exception ->
                    _error.value = exception.message
                }
            )

            _isPosting.value = false
        }
    }

    fun replyToComment(commentId: Long, postId: Long, targetUsername: String, content: String) {
        if (content.isBlank()) return

        val replyContent = if (!content.startsWith("@$targetUsername")) {
            "@$targetUsername $content"
        } else {
            content
        }

        viewModelScope.launch {
            _isPosting.value = true
            _error.value = null

            commentRepository.replyToComment(commentId, replyContent).fold(
                onSuccess = {
                    loadComments(postId)
                },
                onFailure = { exception ->
                    _error.value = exception.message
                }
            )

            _isPosting.value = false
        }
    }

    fun deleteComment(commentId: Long, postId: Long) {
        viewModelScope.launch {
            _error.value = null

            commentRepository.deleteComment(commentId).fold(
                onSuccess = {
                    loadComments(postId)
                },
                onFailure = { exception ->
                    _error.value = exception.message
                }
            )
        }
    }

    fun toggleReplies(commentId: Long, postId: Long) {
        viewModelScope.launch {
            _comments.value = _comments.value.map { commentWithReplies ->
                if (commentWithReplies.comment.id == commentId) {
                    if (!commentWithReplies.isExpanded) {
                        val replies = commentRepository.getCommentRepliesAsList(commentId)
                        commentWithReplies.copy(
                            replies = replies,
                            isExpanded = true
                        )
                    } else {
                        commentWithReplies.copy(isExpanded = false)
                    }
                } else {
                    commentWithReplies
                }
            }
        }
    }

    fun likeComment(commentId: Long, postId: Long) {
        viewModelScope.launch {
            android.util.Log.d("CommentViewModel", "Tentando dar like no comentário: $commentId")
            commentRepository.likeComment(commentId).fold(
                onSuccess = {
                    android.util.Log.d("CommentViewModel", "Like no comentário $commentId com sucesso")
                    _comments.value = _comments.value.map { commentWithReplies ->
                        if (commentWithReplies.comment.id == commentId) {
                            commentWithReplies.copy(
                                comment = commentWithReplies.comment.copy(isLikedByCurrentUser = true)
                            )
                        } else {
                            val updatedReplies = commentWithReplies.replies.map { reply ->
                                if (reply.id == commentId) {
                                    reply.copy(isLikedByCurrentUser = true)
                                } else {
                                    reply
                                }
                            }
                            commentWithReplies.copy(replies = updatedReplies)
                        }
                    }
                },
                onFailure = { exception ->
                    android.util.Log.e("CommentViewModel", "Erro ao dar like no comentário: ${exception.message}", exception)
                    _error.value = exception.message
                }
            )
        }
    }

    fun unlikeComment(commentId: Long, postId: Long) {
        viewModelScope.launch {
            android.util.Log.d("CommentViewModel", "Tentando remover like do comentário: $commentId")
            commentRepository.unlikeComment(commentId).fold(
                onSuccess = {
                    android.util.Log.d("CommentViewModel", "Unlike no comentário $commentId com sucesso")
                    _comments.value = _comments.value.map { commentWithReplies ->
                        if (commentWithReplies.comment.id == commentId) {
                            commentWithReplies.copy(
                                comment = commentWithReplies.comment.copy(isLikedByCurrentUser = false)
                            )
                        } else {
                            val updatedReplies = commentWithReplies.replies.map { reply ->
                                if (reply.id == commentId) {
                                    reply.copy(isLikedByCurrentUser = false)
                                } else {
                                    reply
                                }
                            }
                            commentWithReplies.copy(replies = updatedReplies)
                        }
                    }
                },
                onFailure = { exception ->
                    android.util.Log.e("CommentViewModel", "Erro ao remover like do comentário: ${exception.message}", exception)
                    _error.value = exception.message
                }
            )
        }
    }

    fun clearError() {
        _error.value = null
    }
}

data class CommentWithReplies(
    val comment: CommentModel,
    val replies: List<CommentModel>,
    val isExpanded: Boolean
)
