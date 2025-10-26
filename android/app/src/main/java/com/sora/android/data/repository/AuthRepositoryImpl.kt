package com.sora.android.data.repository

import android.content.Context
import com.sora.android.data.local.SoraDatabase
import com.sora.android.data.local.TokenManager
import com.sora.android.data.local.dao.UserDao
import com.sora.android.data.local.entity.User
import com.sora.android.data.remote.ApiService
import com.sora.android.data.remote.util.NetworkUtils
import com.sora.android.domain.model.*
import com.sora.android.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val userDao: UserDao,
    @ApplicationContext private val context: Context
) : AuthRepository {

    override suspend fun register(request: RegisterRequest): Result<AuthResponseModel> {
        return try {
            val response = apiService.register(request)
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!

                tokenManager.saveTokens(
                    accessToken = authResponse.tokens.accessToken,
                    refreshToken = authResponse.tokens.refreshToken,
                    userId = authResponse.user.id,
                    username = authResponse.user.username
                )

                cacheUser(authResponse.user)

                Result.success(authResponse)
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(request: LoginRequest): Result<AuthResponseModel> {
        return try {
            val response = apiService.login(request)
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!

                tokenManager.saveTokens(
                    accessToken = authResponse.tokens.accessToken,
                    refreshToken = authResponse.tokens.refreshToken,
                    userId = authResponse.user.id,
                    username = authResponse.user.username
                )

                cacheUser(authResponse.user)

                Result.success(authResponse)
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(refreshToken: String): Result<TokenRefreshResponseModel> {
        return try {
            val request = RefreshTokenRequest(refreshToken)
            val response = apiService.refreshToken(request)
            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!

                tokenManager.updateAccessToken(tokenResponse.accessToken)

                Result.success(tokenResponse)
            } else {
                tokenManager.clearTokens()
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            tokenManager.clearTokens()
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            val response = apiService.logout()

            tokenManager.clearTokens()
            clearLocalUserData()

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            tokenManager.clearTokens()
            clearLocalUserData()
            Result.success(Unit)
        }
    }

    override suspend fun getCurrentUser(): Flow<UserModel?> {
        return tokenManager.userId.map { userId ->
            userId?.toLongOrNull()?.let { id ->
                userDao.getUserById(id)?.toUserModel()
            }
        }
    }

    override suspend fun isLoggedIn(): Flow<Boolean> {
        return tokenManager.isLoggedIn
    }

    override suspend fun getAccessToken(): String? {
        return tokenManager.getAccessToken()
    }

    override suspend fun getRefreshToken(): String? {
        return tokenManager.getRefreshToken()
    }

    override suspend fun clearTokens() {
        tokenManager.clearTokens()
        clearLocalUserData()
    }

    private suspend fun cacheUser(authUser: AuthUserModel) {
        val user = User(
            id = authUser.id,
            username = authUser.username,
            email = authUser.email,
            firstName = authUser.firstName,
            lastName = authUser.lastName,
            bio = authUser.bio,
            profilePicture = authUser.profilePicture,
            cacheTimestamp = System.currentTimeMillis(),
            isFullProfile = true
        )
        userDao.insertUser(user)
    }

    private suspend fun clearLocalUserData() {
        val userId = tokenManager.getUserId()
        userId?.let { id ->
            userDao.getUserById(id)?.let { user ->
                userDao.deleteUser(user)
            }
        }
    }

    private fun User.toUserModel(): UserModel {
        return UserModel(
            id = id,
            username = username,
            email = email,
            firstName = firstName,
            lastName = lastName,
            bio = bio,
            profilePicture = profilePicture,
            followersCount = followersCount,
            followingCount = followingCount,
            countriesVisitedCount = countriesVisitedCount
        )
    }
}