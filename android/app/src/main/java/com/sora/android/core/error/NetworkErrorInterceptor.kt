package com.sora.android.core.error

import android.content.Context
import com.sora.android.core.network.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkErrorInterceptor @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    @ApplicationContext private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        return try {
            chain.proceed(request)
        } catch (e: Exception) {
            when (e) {
                is ConnectException,
                is UnknownHostException,
                is SocketTimeoutException,
                is IOException -> {
                    createOfflineResponse(request, e)
                }
                else -> throw e
            }
        }
    }

    private fun createOfflineResponse(request: okhttp3.Request, exception: Exception): Response {
        val errorBody = """{"error":"offline","message":"${exception.message}"}"""
            .toResponseBody("application/json".toMediaTypeOrNull())

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(503)
            .message("Service Unavailable - Offline")
            .body(errorBody)
            .build()
    }
}