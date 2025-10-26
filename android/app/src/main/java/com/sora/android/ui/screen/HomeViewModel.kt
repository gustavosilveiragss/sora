package com.sora.android.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sora.android.debug.DatabaseDebugger
import com.sora.android.domain.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val databaseDebugger: DatabaseDebugger
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadHelloWorld() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            message = null,
            error = null
        )
    }

    fun debugDatabase() {
        databaseDebugger.logTableExists()
        databaseDebugger.logDatabaseContents()
    }
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val message: Message? = null,
    val error: String? = null
)