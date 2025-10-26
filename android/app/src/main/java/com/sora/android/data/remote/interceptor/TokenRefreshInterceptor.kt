package com.sora.android.data.remote.interceptor

import android.util.Log
import com.sora.android.data.local.TokenManager
import com.sora.android.data.remote.ApiService
import com.sora.android.domain.model.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TokenRefreshInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    @Named("refresh") private val refreshApiService: ApiService
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)

        // If we get 401 Unauthorized and it's not an auth endpoint, try to refresh token
        if (response.code == 401 && !isAuthEndpoint(originalRequest.url.pathSegments)) {
            val refreshResult = runBlocking {
                val refreshToken = tokenManager.getRefreshToken()

                if (refreshToken != null) {
                    try {
                        Log.d("TokenRefreshInterceptor", "Attempting to refresh token...")

                        val refreshResponse = refreshApiService.refreshToken(
                            RefreshTokenRequest(refreshToken)
                        )

                        if (refreshResponse.isSuccessful) {
                            refreshResponse.body()?.let { tokenResponse ->
                                tokenManager.updateAccessToken(tokenResponse.accessToken)

                                Log.d("TokenRefreshInterceptor", "Token refreshed successfully")

                                val newRequest = originalRequest.newBuilder()
                                    .removeHeader("Authorization")
                                    .addHeader("Authorization", "Bearer ${tokenResponse.accessToken}")
                                    .build()

                                response.close()
                                return@runBlocking chain.proceed(newRequest)
                            }
                        } else {
                            Log.w("TokenRefreshInterceptor", "Token refresh failed, clearing tokens")
                            tokenManager.clearTokens()
                        }
                    } catch (e: Exception) {
                        Log.e("TokenRefreshInterceptor", "Error refreshing token: ${e.message}")
                        tokenManager.clearTokens()
                    }
                } else {
                    Log.d("TokenRefreshInterceptor", "No refresh token available, clearing tokens")
                    tokenManager.clearTokens()
                }

                return@runBlocking null
            }

            return refreshResult ?: response
        }

        return response
    }

    private fun isAuthEndpoint(pathSegments: List<String>): Boolean {
        return pathSegments.any { segment ->
            segment in listOf("auth", "login", "register", "refresh")
        }
    }
}