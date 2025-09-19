package com.sora.android.domain.repository

import com.sora.android.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun register(request: RegisterRequest): Result<AuthResponseModel>
    suspend fun login(request: LoginRequest): Result<AuthResponseModel>
    suspend fun refreshToken(refreshToken: String): Result<TokenRefreshResponseModel>
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentUser(): Flow<UserModel?>
    suspend fun isLoggedIn(): Flow<Boolean>
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun clearTokens()
}