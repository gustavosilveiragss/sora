package com.sora.android.core.error

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkErrorInterceptor @Inject constructor(
    private val errorManager: ErrorManager,
    @ApplicationContext private val context: Context
) : Interceptor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        return try {
            chain.proceed(request)
        } catch (e: Exception) {
            // Show automatic error snackbar for network issues
            when (e) {
                is ConnectException,
                is UnknownHostException,
                is SocketTimeoutException -> {
                    scope.launch {
                        val (messageKey, _) = errorManager.mapThrowableToErrorPublic(e)
                        val localizedMessage = getLocalizedErrorMessage(messageKey)
                        errorManager.showSnackbar(
                            message = localizedMessage,
                            duration = SnackbarDuration.Long
                        )
                    }
                }
                is IOException -> {
                    scope.launch {
                        val (messageKey, _) = errorManager.mapThrowableToErrorPublic(e)
                        val localizedMessage = getLocalizedErrorMessage(messageKey)
                        errorManager.showSnackbar(
                            message = localizedMessage,
                            duration = SnackbarDuration.Long
                        )
                    }
                }
            }
            throw e
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
            else -> context.getString(com.sora.android.R.string.error_generic)
        }
    }
}