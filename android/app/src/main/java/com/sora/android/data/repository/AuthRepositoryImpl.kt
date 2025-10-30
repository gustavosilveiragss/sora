package com.sora.android.data.repository

import android.content.Context
import android.util.Log
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
            Log.d("SORA_USER", "Iniciando registro de usuario: username=${request.username}, email=${request.email}")

            val response = apiService.register(request)
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                Log.d("SORA_USER", "Registro bem-sucedido: userId=${authResponse.user.id}, username=${authResponse.user.username}")

                Log.d("SORA_USER", "Salvando tokens no TokenManager")
                tokenManager.saveTokens(
                    accessToken = authResponse.tokens.accessToken,
                    refreshToken = authResponse.tokens.refreshToken,
                    userId = authResponse.user.id,
                    username = authResponse.user.username
                )
                Log.d("SORA_USER", "Tokens salvos com sucesso")

                Log.d("SORA_USER", "Cacheando usuario apos registro")
                cacheUser(authResponse.user)

                Result.success(authResponse)
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Log.e("SORA_USER", "Registro falhou: ${response.code()} - $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("SORA_USER", "Excecao ao registrar usuario: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun login(request: LoginRequest): Result<AuthResponseModel> {
        return try {
            Log.d("SORA_USER", "Iniciando login de usuario: email=${request.email}")

            val response = apiService.login(request)
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                Log.d("SORA_USER", "Login bem-sucedido: userId=${authResponse.user.id}, username=${authResponse.user.username}")

                Log.d("SORA_USER", "Salvando tokens no TokenManager")
                tokenManager.saveTokens(
                    accessToken = authResponse.tokens.accessToken,
                    refreshToken = authResponse.tokens.refreshToken,
                    userId = authResponse.user.id,
                    username = authResponse.user.username
                )
                Log.d("SORA_USER", "Tokens salvos com sucesso")

                Log.d("SORA_USER", "Cacheando usuario apos login")
                cacheUser(authResponse.user)

                Result.success(authResponse)
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Log.e("SORA_USER", "Login falhou: ${response.code()} - $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("SORA_USER", "Excecao ao fazer login: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(refreshToken: String): Result<TokenRefreshResponseModel> {
        return try {
            Log.d("SORA_USER", "Tentando refresh do token de acesso")

            val request = RefreshTokenRequest(refreshToken)
            val response = apiService.refreshToken(request)
            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                Log.d("SORA_USER", "Refresh token bem-sucedido")

                Log.d("SORA_USER", "Atualizando access token no TokenManager")
                tokenManager.updateAccessToken(tokenResponse.accessToken)
                Log.d("SORA_USER", "Access token atualizado com sucesso")

                Result.success(tokenResponse)
            } else {
                Log.e("SORA_USER", "Refresh token falhou: ${response.code()}, limpando tokens")
                tokenManager.clearTokens()
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("SORA_USER", "Excecao ao fazer refresh token: ${e.message}, limpando tokens", e)
            tokenManager.clearTokens()
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            Log.d("SORA_USER", "Iniciando logout do usuario")

            val response = apiService.logout()

            Log.d("SORA_USER", "Limpando tokens do TokenManager")
            tokenManager.clearTokens()
            Log.d("SORA_USER", "Limpando dados locais do usuario")
            clearLocalUserData()
            Log.d("SORA_USER", "Logout concluido com sucesso")

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Log.w("SORA_USER", "API logout retornou erro, mas tokens ja foram limpos localmente")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("SORA_USER", "Excecao ao fazer logout: ${e.message}, limpando dados localmente", e)
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
        Log.d("SORA_USER", "Preparando para cachear usuario apos auth: id=${authUser.id}, username=${authUser.username}")

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

        Log.d("SORA_USER", "INSERINDO usuario autenticado na DB local: id=${user.id}, username=${user.username}, email=${user.email}")
        userDao.insertUser(user)
        Log.d("SORA_USER", "Usuario autenticado inserido com sucesso na DB local")
    }

    private suspend fun clearLocalUserData() {
        val userId = tokenManager.getUserId()
        Log.d("SORA_USER", "Limpando dados locais para userId=$userId")

        userId?.let { id ->
            userDao.getUserById(id)?.let { user ->
                Log.d("SORA_USER", "DELETANDO usuario da DB local: id=${user.id}, username=${user.username}")
                userDao.deleteUser(user)
                Log.d("SORA_USER", "Usuario deletado com sucesso da DB local")
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