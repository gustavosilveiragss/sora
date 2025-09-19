package com.sora.android.data.remote.interceptor

import com.sora.android.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRefreshInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)

        // If we get 401 Unauthorized and it's not an auth endpoint, clear tokens to force re-login
        if (response.code == 401 && !isAuthEndpoint(originalRequest.url.pathSegments)) {
            runBlocking {
                tokenManager.clearTokens()
            }
        }

        return response
    }

    private fun isAuthEndpoint(pathSegments: List<String>): Boolean {
        return pathSegments.any { segment ->
            segment in listOf("auth", "login", "register", "refresh")
        }
    }
}