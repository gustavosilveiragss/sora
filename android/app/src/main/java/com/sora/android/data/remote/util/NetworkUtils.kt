package com.sora.android.data.remote.util

import android.content.Context
import com.sora.android.R
import com.sora.android.data.remote.dto.ErrorResponse
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object NetworkUtils {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseErrorMessage(response: Response<*>, context: Context): String {
        return try {
            val errorBodyString = response.errorBody()?.string()
            if (!errorBodyString.isNullOrEmpty()) {
                val errorResponse = json.decodeFromString<ErrorResponse>(errorBodyString)
                errorResponse.message
            } else {
                context.getString(R.string.error_unknown)
            }
        } catch (e: Exception) {
            response.message().ifEmpty { context.getString(R.string.error_unknown) }
        }
    }

    fun parseErrorKey(response: Response<*>): String? {
        return try {
            val errorBodyString = response.errorBody()?.string()
            if (!errorBodyString.isNullOrEmpty()) {
                val errorResponse = json.decodeFromString<ErrorResponse>(errorBodyString)
                errorResponse.messageKey
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun isOfflineError(response: Response<*>): Boolean {
        return response.code() == 503 || response.message().contains("offline", ignoreCase = true)
    }

    fun isNetworkError(throwable: Throwable): Boolean {
        return when (throwable) {
            is ConnectException,
            is UnknownHostException,
            is SocketTimeoutException,
            is IOException -> true
            else -> false
        }
    }
}