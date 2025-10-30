package com.sora.android.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.paging.PagingData
import com.sora.android.data.local.TokenManager
import com.sora.android.data.local.dao.UserDao
import com.sora.android.data.local.dao.CountryDao
import com.sora.android.data.local.dao.CityDao
import com.sora.android.data.remote.ApiService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.InputStream
import com.sora.android.data.remote.dto.UpdateProfileRequest
import com.sora.android.data.remote.dto.ProfilePictureResponse
import com.sora.android.data.remote.dto.toUserStatsModel
import com.sora.android.data.remote.dto.toUserRankingsModel
import com.sora.android.data.remote.dto.toRecentDestinationModel
import com.sora.android.data.remote.util.NetworkUtils
import com.sora.android.domain.model.*
import com.sora.android.R
import com.sora.android.domain.repository.UserRepository
import com.sora.android.data.local.entity.City
import com.sora.android.data.local.entity.Country
import com.sora.android.data.local.entity.User
import com.sora.android.data.remote.dto.CountryDto
import com.sora.android.core.translation.TranslationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val userStatsDao: com.sora.android.data.local.dao.UserStatsDao,
    private val followDao: com.sora.android.data.local.dao.FollowDao,
    private val countryDao: CountryDao,
    private val cityDao: CityDao,
    private val tokenManager: TokenManager,
    private val networkMonitor: com.sora.android.core.network.NetworkMonitor,
    private val translationManager: TranslationManager,
    @ApplicationContext private val context: Context
) : UserRepository {

    override suspend fun getCurrentUserProfile(): Result<UserModel> {
        return try {
            val currentUserId = getCurrentUserId()
            Log.d("SORA_USER", "obterPerfilUsuarioAtual: userId=$currentUserId")

            val cachedUser = userDao.getUserById(currentUserId)
            Log.d("SORA_USER", "Usuario em cache: id=${cachedUser?.id}, bio=${cachedUser?.bio}, tamanho=${cachedUser?.bio?.length}")

            if (cachedUser != null && isCacheValid(cachedUser.cacheTimestamp)) {
                Log.d("SORA_USER", "Cache valido, retornando usuario em cache com bio=${cachedUser.bio}")
                return Result.success(cachedUser.toUserModel())
            }

            Log.d("SORA_USER", "Buscando da API...")
            val response = apiService.getCurrentUserProfile()
            if (response.isSuccessful) {
                response.body()?.let { profile ->
                    Log.d("SORA_USER", "Resposta da API: bio=${profile.bio}, tamanho=${profile.bio?.length}")
                    val userEntity = profile.toUserEntity(cachedUser)
                    Log.d("SORA_USER", "Entidade para salvar: bio=${userEntity.bio}, tamanho=${userEntity.bio?.length}")
                    Log.d("SORA_USER", "INSERINDO usuario na DB local: id=${userEntity.id}, username=${userEntity.username}")
                    userDao.insertUser(userEntity)
                    Log.d("SORA_USER", "Usuario inserido com sucesso na DB local")
                    Result.success(userEntity.toUserModel())
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                Log.w("SORA_USER", "API falhou: ${response.code()}")
                if (cachedUser != null) {
                    Log.d("SORA_USER", "Retornando ao cache com bio=${cachedUser.bio}")
                    Result.success(cachedUser.toUserModel())
                } else {
                    val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: Exception) {
            Log.e("SORA_USER", "Excecao ao obter perfil atual: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getUserTravelStats(userId: Long): Result<TravelStatsModel> {
        return try {
            Log.d("SORA_USER", "obterEstatisticasViagemUsuario: userId=$userId")

            val cachedStats = userStatsDao.getUserStats(userId)
            if (cachedStats != null) {
                val cacheAge = System.currentTimeMillis() - cachedStats.cacheTimestamp
                val cacheAgeHours = cacheAge / (60 * 60 * 1000)
                Log.d("SORA_USER", "CACHE ENCONTRADO: estatisticas para usuario $userId, idade=${cacheAgeHours}h, valido=${isCacheValid(cachedStats.cacheTimestamp)}")
                Log.d("SORA_USER", "Estatisticas em cache: paises=${cachedStats.totalCountriesVisited}, postagens=${cachedStats.totalPostsCount}")

                if (isCacheValid(cachedStats.cacheTimestamp)) {
                    Log.d("SORA_USER", "Cache e valido, retornando imediatamente")
                    return Result.success(cachedStats.toTravelStatsModel())
                }
            } else {
                Log.d("SORA_USER", "SEM CACHE encontrado para usuario $userId")
            }

            Log.d("SORA_USER", "Tentando chamada API...")
            val response = apiService.getUserTravelStats(userId)
            if (response.isSuccessful) {
                response.body()?.let { statsResponse ->
                    Log.d("SORA_USER", "SUCESSO DA API: estatisticas recebidas para ${statsResponse.user.username}")
                    val userStats = statsResponse.toUserStatsModel()
                    Log.d("SORA_USER", "Estatisticas da API: paises=${userStats.travelStats.totalCountriesVisited}, postagens=${userStats.travelStats.totalPostsCount}")

                    val statsEntity = com.sora.android.data.local.entity.CachedUserStats(
                        userId = userId,
                        username = statsResponse.user.username,
                        totalCountriesVisited = userStats.travelStats.totalCountriesVisited,
                        totalCitiesVisited = userStats.travelStats.totalCitiesVisited,
                        totalPostsCount = userStats.travelStats.totalPostsCount,
                        totalLikesReceived = userStats.travelStats.totalLikesReceived,
                        totalCommentsReceived = userStats.travelStats.totalCommentsReceived,
                        totalFollowers = userStats.travelStats.totalFollowers,
                        totalFollowing = userStats.travelStats.totalFollowing,
                        cacheTimestamp = System.currentTimeMillis()
                    )
                    Log.d("SORA_USER", "INSERINDO estatisticas na DB local para usuario $userId")
                    userStatsDao.insertUserStats(statsEntity)
                    Log.d("SORA_USER", "Estatisticas inseridas com sucesso na DB local para usuario $userId")

                    Result.success(userStats.travelStats)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                Log.d("SORA_USER", "API FALHOU: ${response.code()}")
                if (cachedStats != null) {
                    Log.d("SORA_USER", "Retornando ao cached stats mesmo que expiradas")
                    Result.success(cachedStats.toTravelStatsModel())
                } else {
                    val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                    Log.d("SORA_USER", "Sem cache disponivel, retornando erro")
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: Exception) {
            Log.e("SORA_USER", "EXCECAO ao obter estatisticas para usuario $userId: ${e.message}", e)
            val cachedStats = userStatsDao.getUserStats(userId)
            if (cachedStats != null) {
                Log.d("SORA_USER", "Fallback de excecao: returning cached stats")
                Result.success(cachedStats.toTravelStatsModel())
            } else {
                Log.d("SORA_USER", "Excecao e sem cache, retornando falha")
                Result.failure(e)
            }
        }
    }

    override suspend fun getUserProfile(userId: Long): Flow<UserProfileModel?> {
        return flow {
            Log.d("SORA_USER", "obterPerfilUsuario: userId=$userId")

            val cachedUser = userDao.getUserById(userId)

            Log.d("SORA_USER", "CACHE: ${if (cachedUser != null) "Usuario encontrado ${cachedUser.username}" else "Sem cache"}")

            emit(cachedUser?.toUserProfileModel())

            try {
                val response = apiService.getUserById(userId)
                if (response.isSuccessful) {
                    response.body()?.let { profile ->
                        Log.d("SORA_USER", "SUCESSO DA API: armazenando perfil para ${profile.username}")
                        val userEntity = profile.toUserEntity(cachedUser)
                        Log.d("SORA_USER", "INSERINDO usuario na DB local: id=${userEntity.id}, username=${userEntity.username}")
                        userDao.insertUser(userEntity)
                        Log.d("SORA_USER", "Usuario inserido com sucesso na DB local")
                        emit(profile)
                    }
                } else {
                    Log.d("SORA_USER", "API FALHOU: ${response.code()}, mantendo cache")
                }
            } catch (e: Exception) {
                Log.d("SORA_USER", "EXCECAO: ${e.message}, mantendo cache")
            }
        }
    }

    override suspend fun updateUserProfile(request: UpdateProfileRequest): Result<UserProfileModel> {
        return try {
            val response = apiService.updateUserProfile(request)
            if (response.isSuccessful) {
                response.body()?.let { profile ->
                    val userEntity = profile.toUserEntity()
                    userDao.insertUser(userEntity)
                    Result.success(profile)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadProfilePicture(imageUri: String): Result<ProfilePictureResponse> {
        return try {
            Log.d("SORA_USER", "Iniciando upload de foto de perfil para URI: $imageUri")

            val uri = imageUri.toUri()
            val body = when (uri.scheme) {
                "content" -> {
                    Log.d("SORA_USER", "Processando content URI")
                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw Exception("${context.getString(R.string.error_cannot_open_stream)}: $imageUri")

                    val bytes = inputStream.readBytes()
                    inputStream.close()

                    Log.d("SORA_USER", "Lidos ${bytes.size} bytes do content URI")

                    val requestBody = bytes.toRequestBody("image/*".toMediaType())
                    MultipartBody.Part.createFormData("file", "profile_image.jpg", requestBody)
                }
                "file" -> {
                    Log.d("SORA_USER", "Processando file URI")
                    val file = File(uri.path ?: throw Exception(context.getString(R.string.error_invalid_file_path)))
                    if (!file.exists()) {
                        throw Exception("${context.getString(R.string.error_file_not_exist)}: ${file.absolutePath}")
                    }

                    Log.d("SORA_USER", "Arquivo existe: ${file.absolutePath}, tamanho: ${file.length()} bytes")

                    val requestFile = file.asRequestBody("image/*".toMediaType())
                    MultipartBody.Part.createFormData("file", file.name, requestFile)
                }
                else -> {
                    throw Exception("${context.getString(R.string.error_unsupported_uri_scheme)}: ${uri.scheme}")
                }
            }

            Log.d("SORA_USER", "Fazendo chamada API para upload de foto de perfil")
            val response = apiService.uploadProfilePicture(body)

            if (response.isSuccessful) {
                response.body()?.let { pictureResponse ->
                    Log.d("SORA_USER", "Upload bem-sucedido: ${pictureResponse.profilePictureUrl}")

                    val currentUserId = getCurrentUserId()
                    val cachedUser = userDao.getUserById(currentUserId)
                    cachedUser?.let { user ->
                        val updatedUser = user.copy(
                            profilePicture = pictureResponse.profilePictureUrl,
                            cacheTimestamp = System.currentTimeMillis()
                        )
                        Log.d("SORA_USER", "ATUALIZANDO foto de perfil na DB local para usuario $currentUserId")
                        userDao.insertUser(updatedUser)
                        Log.d("SORA_USER", "Foto de perfil atualizada com sucesso na DB local")
                    }

                    Result.success(pictureResponse)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                Log.e("SORA_USER", "Upload falhou: ${response.code()} - ${response.message()}")
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("SORA_USER", "Erro ao fazer upload de foto de perfil: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun searchUsers(
        query: String,
        countryCode: String?,
        page: Int,
        size: Int
    ): Flow<PagingData<UserSearchResultModel>> {
        return flow {
            try {
                val cachedUsers = userDao.getAllUsers()
                    .filter { user ->
                        user.username.contains(query, ignoreCase = true) ||
                        user.firstName.contains(query, ignoreCase = true) ||
                        user.lastName.contains(query, ignoreCase = true)
                    }
                    .take(size)
                    .map { user ->
                        UserSearchResultModel(
                            id = user.id,
                            username = user.username,
                            firstName = user.firstName,
                            lastName = user.lastName,
                            profilePicture = user.profilePicture
                        )
                    }

                if (cachedUsers.isNotEmpty()) {
                    Log.d("SORA_USER", "Cache hit: encontrados ${cachedUsers.size} usuarios correspondentes")
                    emit(PagingData.from(cachedUsers))
                }

                val response = apiService.searchUsers(query, countryCode, page, size)
                if (response.isSuccessful) {
                    response.body()?.let { searchResult ->
                        Log.d("SORA_USER", "API sucesso: cacheando ${searchResult.content.size} usuarios da busca")
                        searchResult.content.forEach { userSearchResult ->
                            val existing = userDao.getUserById(userSearchResult.id)
                            val userEntity = User(
                                id = userSearchResult.id,
                                username = userSearchResult.username,
                                firstName = userSearchResult.firstName,
                                lastName = userSearchResult.lastName,
                                bio = existing?.bio,
                                profilePicture = userSearchResult.profilePicture ?: existing?.profilePicture,
                                followersCount = existing?.followersCount ?: 0,
                                followingCount = existing?.followingCount ?: 0,
                                countriesVisitedCount = existing?.countriesVisitedCount ?: 0,
                                cacheTimestamp = System.currentTimeMillis()
                            )
                            Log.d("SORA_USER", "INSERINDO usuario da busca na DB local: id=${userEntity.id}, username=${userEntity.username}")
                            userDao.insertUser(userEntity)
                        }
                        Log.d("SORA_USER", "Todos usuarios da busca inseridos na DB local")
                        emit(PagingData.from(searchResult.content))
                    }
                } else if (cachedUsers.isEmpty()) {
                    Log.d("SORA_USER", "API falhou e sem cache")
                    emit(PagingData.empty())
                }
            } catch (e: Exception) {
                Log.d("SORA_USER", "Excecao: fallback para cache na busca")
                val cachedUsers = userDao.getAllUsers()
                    .filter { user ->
                        user.username.contains(query, ignoreCase = true) ||
                        user.firstName.contains(query, ignoreCase = true) ||
                        user.lastName.contains(query, ignoreCase = true)
                    }
                    .take(size)
                    .map { user ->
                        UserSearchResultModel(
                            id = user.id,
                            username = user.username,
                            firstName = user.firstName,
                            lastName = user.lastName,
                            profilePicture = user.profilePicture
                        )
                    }

                if (cachedUsers.isNotEmpty()) {
                    emit(PagingData.from(cachedUsers))
                } else {
                    emit(PagingData.empty())
                }
            }
        }
    }

    override suspend fun followUser(userId: Long): Result<FollowModel> {
        return try {
            val response = apiService.followUser(userId)
            if (response.isSuccessful) {
                response.body()?.let { follow ->
                    Result.success(follow)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unfollowUser(userId: Long): Result<Unit> {
        return try {
            val response = apiService.unfollowUser(userId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserFollowers(userId: Long, page: Int, size: Int): Flow<PagingData<UserModel>> {
        return offlineFirstPaging(
            tag = "UserRepository-Followers",
            networkMonitor = networkMonitor,
            getCached = {
                followDao.getUserFollowers(userId).mapNotNull { follow ->
                    userDao.getUserById(follow.followerId)?.toUserModel()
                }
            },
            fetchFromApi = {
                val response = apiService.getUserFollowers(userId, page, size)
                if (response.isSuccessful) {
                    response.body()?.content?.also { followers ->
                        followers.forEach { follower ->
                            val existing = userDao.getUserById(follower.id)
                            val userEntity = User(
                                id = follower.id,
                                username = follower.username,
                                firstName = follower.firstName,
                                lastName = follower.lastName,
                                bio = follower.bio ?: existing?.bio,
                                profilePicture = follower.profilePicture ?: existing?.profilePicture,
                                followersCount = follower.followersCount ?: 0,
                                followingCount = follower.followingCount ?: 0,
                                countriesVisitedCount = follower.countriesVisitedCount ?: 0,
                                cacheTimestamp = System.currentTimeMillis()
                            )
                            Log.d("SORA_USER", "INSERINDO usuario seguidor na DB local: id=${userEntity.id}, username=${userEntity.username}")
                            userDao.insertUser(userEntity)
                        }
                    }
                } else null
            }
        )
    }

    override suspend fun getUserFollowing(userId: Long, page: Int, size: Int): Flow<PagingData<UserModel>> {
        return offlineFirstPaging(
            tag = "UserRepository-Following",
            networkMonitor = networkMonitor,
            getCached = {
                followDao.getUserFollowing(userId).mapNotNull { follow ->
                    userDao.getUserById(follow.followingId)?.toUserModel()
                }
            },
            fetchFromApi = {
                val response = apiService.getUserFollowing(userId, page, size)
                if (response.isSuccessful) {
                    response.body()?.content?.also { following ->
                        following.forEach { user ->
                            val existing = userDao.getUserById(user.id)
                            val userEntity = User(
                                id = user.id,
                                username = user.username,
                                firstName = user.firstName,
                                lastName = user.lastName,
                                bio = user.bio ?: existing?.bio,
                                profilePicture = user.profilePicture ?: existing?.profilePicture,
                                followersCount = user.followersCount ?: 0,
                                followingCount = user.followingCount ?: 0,
                                countriesVisitedCount = user.countriesVisitedCount ?: 0,
                                cacheTimestamp = System.currentTimeMillis()
                            )
                            Log.d("SORA_USER", "INSERINDO usuario seguindo na DB local: id=${userEntity.id}, username=${userEntity.username}")
                            userDao.insertUser(userEntity)
                        }
                    }
                } else null
            }
        )
    }

    override suspend fun getCachedUser(userId: Long): Flow<UserModel?> {
        return userDao.getUserByIdFlow(userId).map { it?.toUserModel() }
    }

    override suspend fun getCachedUsers(): Flow<List<UserModel>> {
        return flow {
            val users = userDao.getAllUsers()
            emit(users.map { it.toUserModel() })
        }
    }

    override suspend fun refreshUserProfile(userId: Long): Result<Unit> {
        return try {
            val response = apiService.getUserById(userId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isCachedUserFollowed(userId: Long): Flow<Boolean> {
        return flow {
            emit(false)
        }
    }

    override suspend fun getUserRankings(userId: Long): Result<UserRankingsModel> {
        return try {
            val response = apiService.getUserRankings(userId)
            if (response.isSuccessful) {
                response.body()?.let { rankings ->
                    Result.success(rankings.toUserRankingsModel(userId))
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecentDestinations(userId: Long, limit: Int): Result<List<RecentDestinationModel>> {
        return try {
            val response = apiService.getRecentDestinations(userId, limit)
            if (response.isSuccessful) {
                response.body()?.let { recentDestResponse ->
                    Log.d("SORA_USER", "Destinos recentes: username=${recentDestResponse.username}, count=${recentDestResponse.recentDestinations.size}")
                    val destinations = recentDestResponse.recentDestinations.map { destDto ->
                        Log.d("SORA_USER", "Processando destino: country=${destDto.country.nameKey}, city=${destDto.lastCityVisited}")

                        val countryEntity = destDto.country.toCountryEntity()
                        try {
                            countryDao.insertCountry(countryEntity)
                        } catch (e: Exception) {
                            Log.w("SORA_USER", "Pais ja existe ou insert falhou: ${e.message}")
                        }

                        if (destDto.lastCityVisited.isNotBlank()) {
                            val cityEntity = City(
                                id = 0,
                                name = destDto.lastCityVisited,
                                nameKey = destDto.lastCityVisited.lowercase().replace(" ", "_"),
                                latitude = destDto.country.latitude ?: 0.0,
                                longitude = destDto.country.longitude ?: 0.0,
                                countryId = destDto.country.id,
                                countryCode = destDto.country.code,
                                cacheTimestamp = System.currentTimeMillis()
                            )
                            try {
                                cityDao.insertCity(cityEntity)
                            } catch (e: Exception) {
                                Log.w("SORA_USER", "Insert de cidade falhou: ${e.message}")
                            }
                        }

                        destDto.toRecentDestinationModel(translationManager)
                    }
                    Result.success(destinations)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("SORA_USER", "Erro ao obter destinos recentes: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getUserFollowersList(userId: Long, page: Int, size: Int): Result<List<UserModel>> {
        return try {
            val cachedFollows = followDao.getUserFollowers(userId)
            val cachedFollowerIds = cachedFollows.map { it.followerId }

            if (cachedFollowerIds.isNotEmpty() && cachedFollows.all { isCacheValid(it.cacheTimestamp) }) {
                val cachedUsers = cachedFollowerIds.mapNotNull { userDao.getUserById(it) }
                if (cachedUsers.size == cachedFollowerIds.size) {
                    Log.d("SORA_USER", "Cache hit: retornando ${cachedUsers.size} seguidores")
                    return Result.success(cachedUsers.map { it.toUserModel() })
                }
            }

            val response = apiService.getUserFollowers(userId, page, size)
            if (response.isSuccessful) {
                val followers = response.body()?.content ?: emptyList()
                Log.d("SORA_USER", "API sucesso: recebidos ${followers.size} seguidores")

                followers.forEach { follower ->
                    val userEntity = User(
                        id = follower.id,
                        username = follower.username,
                        firstName = follower.firstName,
                        lastName = follower.lastName,
                        bio = follower.bio,
                        profilePicture = follower.profilePicture,
                        followersCount = follower.followersCount,
                        followingCount = follower.followingCount,
                        countriesVisitedCount = follower.countriesVisitedCount,
                        cacheTimestamp = System.currentTimeMillis()
                    )
                    Log.d("SORA_USER", "INSERINDO seguidor na DB local: id=${userEntity.id}, username=${userEntity.username}")
                    userDao.insertUser(userEntity)

                    val followEntity = com.sora.android.data.local.entity.Follow(
                        id = generateFollowId(follower.id, userId),
                        followerId = follower.id,
                        followingId = userId,
                        createdAt = "",
                        cacheTimestamp = System.currentTimeMillis()
                    )
                    followDao.insertFollow(followEntity)
                }

                Log.d("SORA_USER", "Cacheados ${followers.size} seguidores com sucesso")
                Result.success(followers)
            } else {
                if (cachedFollowerIds.isNotEmpty()) {
                    val cachedUsers = cachedFollowerIds.mapNotNull { userDao.getUserById(it) }
                    Log.d("SORA_USER", "API falhou: fallback para ${cachedUsers.size} seguidores em cache")
                    Result.success(cachedUsers.map { it.toUserModel() })
                } else {
                    Result.failure(Exception(context.getString(R.string.unknown_error)))
                }
            }
        } catch (e: Exception) {
            Log.e("SORA_USER", "Excecao ao obter seguidores: ${e.message}", e)
            val cachedFollows = followDao.getUserFollowers(userId)
            if (cachedFollows.isNotEmpty()) {
                val cachedUsers = cachedFollows.mapNotNull { userDao.getUserById(it.followerId) }
                Log.d("SORA_USER", "Excecao: fallback para ${cachedUsers.size} seguidores em cache")
                Result.success(cachedUsers.map { it.toUserModel() })
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getUserFollowingList(userId: Long, page: Int, size: Int): Result<List<UserModel>> {
        return try {
            Log.d("SORA_USER", "Obtendo usuarios seguidos por usuario $userId")

            val cachedFollows = followDao.getUserFollowing(userId)
            val cachedFollowingIds = cachedFollows.map { it.followingId }

            if (cachedFollowingIds.isNotEmpty() && cachedFollows.all { isCacheValid(it.cacheTimestamp) }) {
                val cachedUsers = cachedFollowingIds.mapNotNull { userDao.getUserById(it) }
                if (cachedUsers.size == cachedFollowingIds.size) {
                    Log.d("SORA_USER", "Cache hit: retornando ${cachedUsers.size} usuarios seguidos")
                    return Result.success(cachedUsers.map { it.toUserModel() })
                }
            }

            val response = apiService.getUserFollowing(userId, page, size)
            if (response.isSuccessful) {
                val following = response.body()?.content ?: emptyList()
                Log.d("SORA_USER", "API sucesso: recebidos ${following.size} usuarios seguidos")

                following.forEach { followedUser ->
                    val userEntity = User(
                        id = followedUser.id,
                        username = followedUser.username,
                        firstName = followedUser.firstName,
                        lastName = followedUser.lastName,
                        bio = followedUser.bio,
                        profilePicture = followedUser.profilePicture,
                        followersCount = followedUser.followersCount,
                        followingCount = followedUser.followingCount,
                        countriesVisitedCount = followedUser.countriesVisitedCount,
                        cacheTimestamp = System.currentTimeMillis()
                    )
                    Log.d("SORA_USER", "INSERINDO usuario seguido na DB local: id=${userEntity.id}, username=${userEntity.username}")
                    userDao.insertUser(userEntity)

                    val followEntity = com.sora.android.data.local.entity.Follow(
                        id = generateFollowId(userId, followedUser.id),
                        followerId = userId,
                        followingId = followedUser.id,
                        createdAt = "",
                        cacheTimestamp = System.currentTimeMillis()
                    )
                    followDao.insertFollow(followEntity)
                }

                Log.d("SORA_USER", "Cacheados ${following.size} usuarios seguidos com sucesso")
                Result.success(following)
            } else {
                if (cachedFollowingIds.isNotEmpty()) {
                    val cachedUsers = cachedFollowingIds.mapNotNull { userDao.getUserById(it) }
                    Log.d("SORA_USER", "API falhou: fallback para ${cachedUsers.size} usuarios seguidos em cache")
                    Result.success(cachedUsers.map { it.toUserModel() })
                } else {
                    Log.e("SORA_USER", "API falhou sem cache disponivel")
                    Result.failure(Exception(context.getString(R.string.unknown_error)))
                }
            }
        } catch (e: Exception) {
            Log.e("SORA_USER", "Excecao ao obter usuarios seguidos: ${e.message}", e)
            val cachedFollows = followDao.getUserFollowing(userId)
            if (cachedFollows.isNotEmpty()) {
                val cachedUsers = cachedFollows.mapNotNull { userDao.getUserById(it.followingId) }
                Log.d("SORA_USER", "Excecao: fallback para ${cachedUsers.size} usuarios seguidos em cache")
                Result.success(cachedUsers.map { it.toUserModel() })
            } else {
                Result.failure(e)
            }
        }
    }

    private suspend fun getCurrentUserId(): Long {
        return tokenManager.getUserId() ?: 1L
    }

    private fun generateFollowId(followerId: Long, followingId: Long): Long {
        return (followerId.toString() + followingId.toString()).hashCode().toLong()
    }
}

private fun isCacheValid(timestamp: Long): Boolean {
    val cacheExpiryMs = 12 * 60 * 60 * 1000L
    return (System.currentTimeMillis() - timestamp) < cacheExpiryMs
}

private fun UserProfileModel.toUserEntity(existingUser: User? = null): User {
    return User(
        id = id,
        username = username,
        firstName = firstName,
        lastName = lastName,
        bio = bio ?: existingUser?.bio,
        profilePicture = profilePicture ?: existingUser?.profilePicture,
        followersCount = followersCount,
        followingCount = followingCount,
        countriesVisitedCount = countriesVisitedCount,
        cacheTimestamp = System.currentTimeMillis()
    )
}

private fun User.toUserModel(): UserModel {
    return UserModel(
        id = id,
        username = username,
        firstName = firstName,
        lastName = lastName,
        bio = bio,
        profilePicture = profilePicture
    )
}

private fun User.toUserProfileModel(): UserProfileModel {
    return UserProfileModel(
        id = id,
        username = username,
        firstName = firstName,
        lastName = lastName,
        bio = bio,
        profilePicture = profilePicture,
        followersCount = followersCount,
        followingCount = followingCount,
        countriesVisitedCount = countriesVisitedCount
    )
}

private fun RankingsModel.toUserRankingsModel(userId: Long): UserRankingsModel {
    return UserRankingsModel(
        userId = userId,
        countriesRankPosition = countriesRankAmongMutuals?.position,
        postsRankPosition = postsRankAmongMutuals?.position,
        likesRankPosition = null,
        totalUsers = countriesRankAmongMutuals?.totalUsers ?: postsRankAmongMutuals?.totalUsers ?: 0,
        percentileCountries = countriesRankAmongMutuals?.percentile,
        percentilePosts = postsRankAmongMutuals?.percentile,
        percentileLikes = null
    )
}

private fun CountryDto.toCountryEntity(): Country {
    return Country(
        id = id,
        code = code,
        nameKey = nameKey,
        latitude = latitude,
        longitude = longitude,
        cacheTimestamp = System.currentTimeMillis()
    )
}

private fun com.sora.android.data.local.entity.CachedUserStats.toTravelStatsModel(): TravelStatsModel {
    return TravelStatsModel(
        totalCountriesVisited = totalCountriesVisited,
        totalCitiesVisited = totalCitiesVisited,
        totalPostsCount = totalPostsCount,
        totalLikesReceived = totalLikesReceived,
        totalCommentsReceived = totalCommentsReceived,
        totalFollowers = totalFollowers,
        totalFollowing = totalFollowing
    )
}

