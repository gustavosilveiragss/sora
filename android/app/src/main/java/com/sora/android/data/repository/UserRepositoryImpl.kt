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
    private val countryDao: CountryDao,
    private val cityDao: CityDao,
    private val tokenManager: TokenManager,
    private val translationManager: TranslationManager,
    @ApplicationContext private val context: Context
) : UserRepository {

    override suspend fun getCurrentUserProfile(): Result<UserModel> {
        return try {
            val currentUserId = getCurrentUserId()
            val cachedUser = userDao.getUserById(currentUserId)

            if (cachedUser != null && isCacheValid(cachedUser.cacheTimestamp)) {
                return Result.success(cachedUser.toUserModel())
            }

            val response = apiService.getCurrentUserProfile()
            if (response.isSuccessful) {
                response.body()?.let { profile ->
                    val userEntity = profile.toUserEntity()
                    userDao.insertUser(userEntity)
                    Result.success(userEntity.toUserModel())
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                if (cachedUser != null) {
                    Result.success(cachedUser.toUserModel())
                } else {
                    val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserTravelStats(userId: Long): Result<TravelStatsModel> {
        return try {
            Log.d("UserRepository", "=== DATABASE DEBUG ===")
            debugDatabase()

            val response = apiService.getUserTravelStats(userId)
            if (response.isSuccessful) {
                response.body()?.let { statsResponse ->
                    Log.d("UserRepository", "Received stats from API - user: ${statsResponse.user.username}")
                    val userStats = statsResponse.toUserStatsModel()
                    Result.success(userStats.travelStats)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserProfile(userId: Long): Flow<UserProfileModel?> {
        return flow {
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null && isCacheValid(cachedUser.cacheTimestamp)) {
                emit(cachedUser.toUserProfileModel())
            }

            try {
                val response = apiService.getUserById(userId)
                if (response.isSuccessful) {
                    response.body()?.let { profile ->
                        val userEntity = profile.toUserEntity()
                        userDao.insertUser(userEntity)
                        emit(profile)
                    }
                } else if (cachedUser != null) {
                    emit(cachedUser.toUserProfileModel())
                }
            } catch (e: Exception) {
                if (cachedUser != null) {
                    emit(cachedUser.toUserProfileModel())
                } else {
                    emit(null)
                }
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
            Log.d("UserRepository", "Starting upload for URI: $imageUri")

            val uri = imageUri.toUri()
            val body = when (uri.scheme) {
                "content" -> {
                    Log.d("UserRepository", "Handling content URI")
                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw Exception("${context.getString(R.string.error_cannot_open_stream)}: $imageUri")

                    val bytes = inputStream.readBytes()
                    inputStream.close()

                    Log.d("UserRepository", "Read ${bytes.size} bytes from content URI")

                    val requestBody = bytes.toRequestBody("image/*".toMediaType())
                    MultipartBody.Part.createFormData("file", "profile_image.jpg", requestBody)
                }
                "file" -> {
                    Log.d("UserRepository", "Handling file URI")
                    val file = File(uri.path ?: throw Exception(context.getString(R.string.error_invalid_file_path)))
                    if (!file.exists()) {
                        throw Exception("${context.getString(R.string.error_file_not_exist)}: ${file.absolutePath}")
                    }

                    Log.d("UserRepository", "File exists: ${file.absolutePath}, size: ${file.length()} bytes")

                    val requestFile = file.asRequestBody("image/*".toMediaType())
                    MultipartBody.Part.createFormData("file", file.name, requestFile)
                }
                else -> {
                    throw Exception("${context.getString(R.string.error_unsupported_uri_scheme)}: ${uri.scheme}")
                }
            }

            Log.d("UserRepository", "Making API call to upload profile picture")
            val response = apiService.uploadProfilePicture(body)

            if (response.isSuccessful) {
                response.body()?.let { pictureResponse ->
                    Log.d("UserRepository", "Upload successful: ${pictureResponse.profilePictureUrl}")

                    val currentUserId = getCurrentUserId()
                    val cachedUser = userDao.getUserById(currentUserId)
                    cachedUser?.let { user ->
                        val updatedUser = user.copy(
                            profilePicture = pictureResponse.profilePictureUrl,
                            cacheTimestamp = System.currentTimeMillis()
                        )
                        userDao.insertUser(updatedUser)
                    }

                    Result.success(pictureResponse)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                Log.e("UserRepository", "Upload failed: ${response.code()} - ${response.message()}")
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error uploading profile picture: ${e.message}", e)
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
                val response = apiService.searchUsers(query, countryCode, page, size)
                if (response.isSuccessful) {
                    response.body()?.let { searchResult ->
                        emit(PagingData.from(searchResult.content))
                    }
                } else {
                    emit(PagingData.empty())
                }
            } catch (e: Exception) {
                emit(PagingData.empty())
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
        return flow {
            try {
                val response = apiService.getUserFollowers(userId, page, size)
                if (response.isSuccessful) {
                    response.body()?.content?.let { followers ->
                        emit(PagingData.from(followers))
                    }
                } else {
                    emit(PagingData.empty())
                }
            } catch (e: Exception) {
                emit(PagingData.empty())
            }
        }
    }

    override suspend fun getUserFollowing(userId: Long, page: Int, size: Int): Flow<PagingData<UserModel>> {
        return flow {
            try {
                val response = apiService.getUserFollowing(userId, page, size)
                if (response.isSuccessful) {
                    response.body()?.content?.let { following ->
                        emit(PagingData.from(following))
                    }
                } else {
                    emit(PagingData.empty())
                }
            } catch (e: Exception) {
                emit(PagingData.empty())
            }
        }
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
                    Log.d("UserRepository", "Recent destinations response: username=${recentDestResponse.username}, count=${recentDestResponse.recentDestinations.size}")
                    val destinations = recentDestResponse.recentDestinations.map { destDto ->
                        Log.d("UserRepository", "Processing destination: country=${destDto.country.nameKey}, city=${destDto.lastCityVisited}")

                        val countryEntity = destDto.country.toCountryEntity()
                        try {
                            countryDao.insertCountry(countryEntity)
                        } catch (e: Exception) {
                            Log.w("UserRepository", "Country already exists or insert failed: ${e.message}")
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
                                Log.w("UserRepository", "City insert failed: ${e.message}")
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
            Log.e("UserRepository", "Error getting recent destinations: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getUserFollowersList(userId: Long, page: Int, size: Int): Result<List<UserModel>> {
        return try {
            val response = apiService.getUserFollowers(userId, page, size)
            if (response.isSuccessful) {
                val followers = response.body()?.content ?: emptyList()
                Result.success(followers)
            } else {
                Result.failure(Exception(context.getString(R.string.unknown_error)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserFollowingList(userId: Long, page: Int, size: Int): Result<List<UserModel>> {
        return try {
            val response = apiService.getUserFollowing(userId, page, size)
            if (response.isSuccessful) {
                val following = response.body()?.content ?: emptyList()
                Result.success(following)
            } else {
                Result.failure(Exception(context.getString(R.string.unknown_error)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getCurrentUserId(): Long {
        return tokenManager.getUserId() ?: 1L
    }

    private suspend fun debugDatabase() {
        try {
            val userCount = userDao.getAllUsers().size
            Log.d("UserRepository", "DATABASE STATUS:")
            Log.d("UserRepository", "- Users in database: $userCount")

            if (userCount > 0) {
                val users = userDao.getAllUsers().take(3)
                users.forEach { user ->
                    Log.d("UserRepository", "- User: ${user.username} (ID: ${user.id}) - cached: ${System.currentTimeMillis() - user.cacheTimestamp}ms ago")
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error reading database: ${e.message}", e)
        }
    }

}

private fun isCacheValid(timestamp: Long): Boolean {
    val cacheExpiryMs = 12 * 60 * 60 * 1000L
    return (System.currentTimeMillis() - timestamp) < cacheExpiryMs
}

private fun UserProfileModel.toUserEntity(): User {
    return User(
        id = id,
        username = username,
        firstName = firstName,
        lastName = lastName,
        bio = bio,
        profilePicture = profilePicture,
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

