package com.sora.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sora.android.data.local.SoraDatabase
import com.sora.android.data.local.TokenManager
import com.sora.android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoggingOut: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val database: SoraDatabase,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun logout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingOut = true)

            try {
                CoroutineScope(Dispatchers.IO).launch {
                    database.clearAllTables()
                }.join()
                authRepository.logout()
                tokenManager.clearAll()
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.IO).launch {
                    database.clearAllTables()
                }.join()
                tokenManager.clearAll()
            }

            _uiState.value = _uiState.value.copy(isLoggingOut = false)
        }
    }
}