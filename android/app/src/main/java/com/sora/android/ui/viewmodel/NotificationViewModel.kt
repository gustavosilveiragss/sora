package com.sora.android.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.sora.android.R
import com.sora.android.data.local.TokenManager
import com.sora.android.domain.model.NotificationModel
import com.sora.android.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    data class NotificationUiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val unreadCount: Int = 0,
        val showUnreadOnly: Boolean = false,
        val currentUserId: Long? = null
    )

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    val notifications: Flow<PagingData<NotificationModel>> = _refreshTrigger.flatMapLatest {
        notificationRepository.getNotifications(
            unreadOnly = _uiState.value.showUnreadOnly,
            type = null,
            page = 0,
            size = 20
        )
    }.cachedIn(viewModelScope)

    private val _forceRefreshUnreadCount = MutableStateFlow(0)

    init {
        loadCurrentUserId()
        observeUnreadCount()
    }

    private fun loadCurrentUserId() {
        viewModelScope.launch {
            val userId = tokenManager.getUserId()
            _uiState.update { it.copy(currentUserId = userId) }
        }
    }

    private fun observeUnreadCount() {
        viewModelScope.launch {
            _forceRefreshUnreadCount.flatMapLatest {
                notificationRepository.getUnreadCount()
            }.collect { count ->
                Log.d("SORA_NOTIFICATION", "Contagem de nao lidas atualizada: $count")
                _uiState.update { it.copy(unreadCount = count) }
            }
        }
    }

    fun loadUnreadCount() {
        Log.d("SORA_NOTIFICATION", "Forcando atualizacao de contagem de nao lidas")
        _forceRefreshUnreadCount.value++
    }

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            try {
                Log.d("SORA_NOTIFICATION", "Marcando notificacao como lida: $notificationId")
                _uiState.update { it.copy(isLoading = true, error = null) }

                notificationRepository.markNotificationAsRead(notificationId)
                    .onSuccess {
                        Log.d("SORA_NOTIFICATION", "Notificacao marcada como lida com sucesso")

                        _uiState.update {
                            val newCount = maxOf(0, it.unreadCount - 1)
                            Log.d("SORA_NOTIFICATION", "Atualizando contador: ${it.unreadCount} -> $newCount")
                            it.copy(unreadCount = newCount)
                        }

                        Log.d("SORA_NOTIFICATION", "Contador atual apos update: ${_uiState.value.unreadCount}")
                        refresh()
                    }
                    .onFailure { error ->
                        Log.e("SORA_NOTIFICATION", "Erro ao marcar como lida: ${error.message}")
                        _uiState.update {
                            it.copy(
                                error = error.message ?: context.getString(R.string.error_marking_as_read)
                            )
                        }
                    }

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Log.e("SORA_NOTIFICATION", "Excecao ao marcar como lida: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: context.getString(R.string.error_marking_as_read)
                    )
                }
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                Log.d("SORA_NOTIFICATION", "Marcando todas as notificacoes como lidas")
                _uiState.update { it.copy(isLoading = true, error = null) }

                notificationRepository.markAllNotificationsAsRead()
                    .onSuccess {
                        Log.d("SORA_NOTIFICATION", "Todas as notificacoes marcadas como lidas")

                        _uiState.update { it.copy(unreadCount = 0) }

                        refresh()
                    }
                    .onFailure { error ->
                        Log.e("SORA_NOTIFICATION", "Erro ao marcar todas como lidas: ${error.message}")
                        _uiState.update {
                            it.copy(
                                error = error.message ?: context.getString(R.string.error_marking_as_read)
                            )
                        }
                    }

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Log.e("SORA_NOTIFICATION", "Excecao ao marcar todas como lidas: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: context.getString(R.string.error_marking_as_read)
                    )
                }
            }
        }
    }

    fun toggleUnreadFilter() {
        _uiState.update { it.copy(showUnreadOnly = !it.showUnreadOnly) }
        refresh()
    }

    fun refresh() {
        Log.d("SORA_NOTIFICATION", "Atualizando lista de notificacoes")
        _refreshTrigger.value++
        loadUnreadCount()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
