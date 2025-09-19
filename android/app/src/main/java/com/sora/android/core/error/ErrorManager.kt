package com.sora.android.core.error

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

data class ErrorState(
    val message: String = "",
    val type: ErrorType = ErrorType.GENERIC,
    val isVisible: Boolean = false,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

enum class ErrorType {
    NETWORK_CONNECTION,
    NETWORK_TIMEOUT,
    SERVER_UNAVAILABLE,
    INVALID_CREDENTIALS,
    VALIDATION,
    GENERIC,
    UNKNOWN
}

@Singleton
class ErrorManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _errorState = MutableStateFlow(ErrorState())
    val errorState: StateFlow<ErrorState> = _errorState.asStateFlow()

    val snackbarHostState = SnackbarHostState()

    fun showError(
        message: String,
        type: ErrorType = ErrorType.GENERIC,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        _errorState.value = ErrorState(
            message = message,
            type = type,
            isVisible = true,
            actionLabel = actionLabel,
            onAction = onAction
        )
    }

    fun showError(throwable: Throwable, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        val (message, type) = mapThrowableToError(throwable)
        showError(message, type, actionLabel, onAction)
    }

    suspend fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short
    ): SnackbarResult {
        return snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = duration
        )
    }

    suspend fun showErrorSnackbar(
        throwable: Throwable,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Long
    ): SnackbarResult {
        val localizedMessage = when {
            throwable.message?.startsWith("error_") == true -> {
                getLocalizedErrorMessage(throwable.message!!)
            }
            else -> {
                throwable.message ?: context.getString(com.sora.android.R.string.error_generic)
            }
        }
        return showSnackbar(localizedMessage, actionLabel, duration)
    }

    fun hideError() {
        _errorState.value = _errorState.value.copy(isVisible = false)
    }

    fun clearError() {
        _errorState.value = ErrorState()
    }

    fun mapThrowableToErrorPublic(throwable: Throwable): Pair<String, ErrorType> = mapThrowableToError(throwable)

    private fun mapThrowableToError(throwable: Throwable): Pair<String, ErrorType> {
        return when (throwable) {
            is ConnectException, is UnknownHostException -> {
                Pair("error_network_connection", ErrorType.NETWORK_CONNECTION)
            }
            is SocketTimeoutException -> {
                Pair("error_network_timeout", ErrorType.NETWORK_TIMEOUT)
            }
            is retrofit2.HttpException -> {
                when (throwable.code()) {
                    401 -> Pair("error_invalid_credentials", ErrorType.INVALID_CREDENTIALS)
                    400 -> Pair("error_validation", ErrorType.VALIDATION)
                    500, 502, 503, 504 -> Pair("error_server_unavailable", ErrorType.SERVER_UNAVAILABLE)
                    else -> Pair("error_generic", ErrorType.GENERIC)
                }
            }
            else -> {
                Pair(throwable.message ?: "error_unknown", ErrorType.UNKNOWN)
            }
        }
    }

    private fun getLocalizedErrorMessage(messageKey: String): String {
        return when (messageKey) {
            "error_network_connection" -> context.getString(com.sora.android.R.string.error_network_connection)
            "error_network_timeout" -> context.getString(com.sora.android.R.string.error_network_timeout)
            "error_server_unavailable" -> context.getString(com.sora.android.R.string.error_server_unavailable)
            "error_invalid_credentials" -> context.getString(com.sora.android.R.string.error_invalid_credentials)
            "error_validation" -> context.getString(com.sora.android.R.string.error_validation)
            "error_unknown" -> context.getString(com.sora.android.R.string.error_unknown)
            else -> messageKey.takeIf { !it.startsWith("error_") } ?: context.getString(com.sora.android.R.string.error_generic)
        }
    }
}