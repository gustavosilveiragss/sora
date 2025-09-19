package com.sora.android.data.remote.interceptor

import com.sora.android.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val isAuthEndpoint = originalRequest.url.pathSegments.any { segment ->
            segment in listOf("auth", "login", "register")
        }

        if (isAuthEndpoint) {
            return chain.proceed(originalRequest)
        }

        val accessToken = runBlocking {
            tokenManager.getAccessToken()
        }

        val requestWithAuth = if (accessToken != null) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(requestWithAuth)
    }
}