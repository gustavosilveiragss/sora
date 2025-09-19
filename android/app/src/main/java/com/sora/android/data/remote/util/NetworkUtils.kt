package com.sora.android.data.remote.util

import com.sora.android.data.remote.dto.ErrorResponse
import kotlinx.serialization.json.Json
import retrofit2.Response

object NetworkUtils {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseErrorMessage(response: Response<*>): String {
        return try {
            val errorBodyString = response.errorBody()?.string()
            if (!errorBodyString.isNullOrEmpty()) {
                val errorResponse = json.decodeFromString<ErrorResponse>(errorBodyString)
                errorResponse.message
            } else {
                "Erro desconhecido"
            }
        } catch (e: Exception) {
            response.message().ifEmpty { "Erro desconhecido" }
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
}