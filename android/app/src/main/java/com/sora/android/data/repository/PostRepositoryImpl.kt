package com.sora.android.data.repository

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.sora.android.data.local.TokenManager
import com.sora.android.data.local.dao.PostDao
import com.sora.android.data.local.entity.Post
import com.sora.android.data.remote.ApiService
import com.sora.android.data.remote.dto.LikeCreateResponse
import com.sora.android.data.remote.dto.MediaUploadResponse
import com.sora.android.data.remote.util.NetworkUtils
import com.sora.android.domain.model.*
import com.sora.android.domain.repository.PostRepository
import com.sora.android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val postDao: PostDao,
    private val userDao: com.sora.android.data.local.dao.UserDao,
    private val tokenManager: TokenManager,
    private val networkMonitor: com.sora.android.core.network.NetworkMonitor,
    @ApplicationContext private val context: Context
) : PostRepository {

    override suspend fun getPostById(id: Long): Flow<PostModel?> {
        return flow {
            val cachedPost = postDao.getPostById(id)
            if (cachedPost != null && isCacheValid(cachedPost.cacheTimestamp)) {
                emit(cachedPost.toPostModel())
            }

            try {
                val response = apiService.getPostById(id)
                if (response.isSuccessful) {
                    response.body()?.let { post ->
                        val postEntity = post.toPostEntity()
                        postDao.insertPost(postEntity)
                        emit(post)
                    }
                } else if (cachedPost != null) {
                    emit(cachedPost.toPostModel())
                }
            } catch (e: Exception) {
                if (cachedPost != null) {
                    emit(cachedPost.toPostModel())
                }
            }
        }
    }

    override suspend fun createPost(request: PostCreateRequest): Result<List<PostModel>> {
        return try {
            val response = apiService.createPost(request)
            if (response.isSuccessful) {
                response.body()?.let { posts ->
                    val postEntities = posts.map { it.toPostEntity() }
                    postDao.insertPosts(postEntities)
                    Result.success(posts)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePost(postId: Long, request: PostUpdateRequest): Result<PostModel> {
        return try {
            val response = apiService.updatePost(postId, request)
            if (response.isSuccessful) {
                response.body()?.let { post ->
                    val postEntity = post.toPostEntity()
                    postDao.updatePost(postEntity)
                    Result.success(post)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePost(postId: Long): Result<Unit> {
        return try {
            val response = apiService.deletePost(postId)
            if (response.isSuccessful) {
                postDao.deletePostById(postId)
                Result.success(Unit)
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFeed(page: Int, size: Int): Flow<PagingData<PostModel>> {
        return Pager(
            config = PagingConfig(
                pageSize = size,
                enablePlaceholders = false,
                prefetchDistance = 3
            ),
            pagingSourceFactory = {
                com.sora.android.data.paging.FeedPagingSource(
                    apiService = apiService,
                    postDao = postDao,
                    userDao = userDao,
                    tokenManager = tokenManager,
                    networkMonitor = networkMonitor
                )
            }
        ).flow
    }

    override suspend fun getExplorePosts(timeframe: String, page: Int, size: Int): Flow<PagingData<PostModel>> {
        return Pager(
            config = PagingConfig(
                pageSize = size,
                enablePlaceholders = false,
                prefetchDistance = 3
            ),
            pagingSourceFactory = {
                com.sora.android.data.paging.ExplorePagingSource(
                    apiService = apiService,
                    postDao = postDao,
                    userDao = userDao,
                    networkMonitor = networkMonitor,
                    timeframe = timeframe
                )
            }
        ).flow
    }

    override suspend fun getUserPosts(userId: Long, page: Int, size: Int): Flow<PagingData<PostModel>> {
        return flow {
            android.util.Log.d("PostRepository", "getUserPosts: userId=$userId")

            val cachedPosts = postDao.getRecentPosts(200)
                .filter { it.profileOwnerId == userId || it.authorId == userId }
                .sortedByDescending { it.createdAt }

            android.util.Log.d("PostRepository", "CACHE: Encontrado ${cachedPosts.size} user posts")

            if (!networkMonitor.isCurrentlyConnected()) {
                android.util.Log.d("PostRepository", "OFFLINE: Emitting ${cachedPosts.size} posts and stopping")
                emit(PagingData.from(cachedPosts.map { it.toPostModel() }))
                return@flow
            }

            val cacheIsValid = cachedPosts.isNotEmpty() && isCacheValid(cachedPosts.first().cacheTimestamp)
            if (cacheIsValid) {
                android.util.Log.d("PostRepository", "ONLINE + Valid cache: Emitting ${cachedPosts.size} posts first")
                emit(PagingData.from(cachedPosts.map { it.toPostModel() }))
            }

            try {
                android.util.Log.d("PostRepository", "Fetching country collections from API...")
                val countriesResponse = apiService.getUserCountryCollections(userId)
                if (countriesResponse.isSuccessful) {
                    countriesResponse.body()?.let { countryCollections ->
                        android.util.Log.d("PostRepository", "User has ${countryCollections.countries.size} countries")
                        val allPosts = mutableListOf<PostModel>()

                        countryCollections.countries.forEach { country ->
                            val postsResponse = apiService.getCountryPosts(
                                userId = userId,
                                countryCode = country.countryCode,
                                page = 0,
                                size = 20
                            )
                            if (postsResponse.isSuccessful) {
                                postsResponse.body()?.posts?.content?.let { posts ->
                                    allPosts.addAll(posts)
                                    postDao.insertPosts(posts.map { it.toPostEntity() })
                                }
                            }
                        }

                        if (allPosts.isNotEmpty()) {
                            android.util.Log.d("PostRepository", "SUCESSO DA API: ${allPosts.size} posts")
                            emit(PagingData.from(allPosts.sortedByDescending { it.createdAt }))
                        } else if (!cacheIsValid && cachedPosts.isEmpty()) {
                            emit(PagingData.empty())
                        }
                    }
                } else if (!cacheIsValid && cachedPosts.isNotEmpty()) {
                    android.util.Log.d("PostRepository", "API FALHOU: Using expired cache")
                    emit(PagingData.from(cachedPosts.map { it.toPostModel() }))
                } else if (cachedPosts.isEmpty()) {
                    emit(PagingData.empty())
                }
            } catch (e: Exception) {
                android.util.Log.e("PostRepository", "EXCEPTION: ${e.message}")
                if (!cacheIsValid && cachedPosts.isNotEmpty()) {
                    emit(PagingData.from(cachedPosts.map { it.toPostModel() }))
                } else if (cachedPosts.isEmpty()) {
                    emit(PagingData.empty())
                }
            }
        }
    }

    override suspend fun getCountryPosts(
        userId: Long,
        countryCode: String,
        collectionCode: String?,
        cityName: String?,
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): Flow<PagingData<PostModel>> {
        val tag = "PostRepository-Country-$countryCode"
        android.util.Log.d(tag, "INÍCIO getCountryPosts: userId=$userId, collection=$collectionCode, city=$cityName")

        return offlineFirstPaging(
            tag = tag,
            networkMonitor = networkMonitor,
            getCached = {
                val cached = when {
                    cityName != null -> postDao.getCityPostsList(countryCode, userId, cityName)
                    collectionCode != null -> postDao.getCountryCollectionPostsList(countryCode, userId, collectionCode)
                    else -> postDao.getCountryPostsList(countryCode, userId)
                }
                android.util.Log.d(tag, "CONSULTA CACHE CONCLUÍDA: Encontrado ${cached.size} posts, convertendo...")
                val result = cached.map { it.toPostModel() }
                android.util.Log.d(tag, "CACHE CONVERTIDO: ${result.size} PostModels prontos")
                result
            },
            fetchFromApi = {
                android.util.Log.d(tag, "API CALL STARTING...")
                val response = apiService.getCountryPosts(
                    userId, countryCode, collectionCode, cityName, page, size, sortBy, sortDirection
                )
                android.util.Log.d(tag, "API RESPONSE CODE: ${response.code()}")
                if (response.isSuccessful) {
                    response.body()?.posts?.content?.also { posts ->
                        android.util.Log.d(tag, "SUCESSO DA API: Got ${posts.size} posts, caching...")
                        postDao.insertPosts(posts.map { it.toPostEntity() })
                        android.util.Log.d(tag, "CACHE INSERT DONE")
                    }
                } else {
                    android.util.Log.w(tag, "API FALHOU: ${response.code()}")
                    null
                }
            }
        )
    }

    override suspend fun getSharedPostGroup(groupId: String): Flow<List<PostModel>> {
        return flow {
            val cachedPosts = postDao.getPostsBySharedGroup(groupId)
            if (cachedPosts.isNotEmpty()) {
                emit(cachedPosts.map { it.toPostModel() })
            }

            try {
                val response = apiService.getSharedPostGroup(groupId)
                if (response.isSuccessful) {
                    response.body()?.let { posts ->
                        val postEntities = posts.map { it.toPostEntity() }
                        postEntities.forEach { postDao.insertPost(it) }
                        emit(posts)
                    }
                } else if (cachedPosts.isNotEmpty()) {
                    emit(cachedPosts.map { it.toPostModel() })
                }
            } catch (e: Exception) {
                if (cachedPosts.isNotEmpty()) {
                    emit(cachedPosts.map { it.toPostModel() })
                } else {
                    emit(emptyList())
                }
            }
        }
    }

    override suspend fun uploadPostMedia(postId: Long, imageUris: List<String>): Result<MediaUploadResponse> {
        return try {
            android.util.Log.d("SORA_POST", "Iniciando upload de ${imageUris.size} imagens para post $postId")

            val parts = imageUris.mapIndexed { index, uriString ->
                val uri = android.net.Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception(context.getString(R.string.error_cannot_open_stream))

                val fileName = "image_${index}_${System.currentTimeMillis()}.jpg"
                val requestBody = inputStream.readBytes().toRequestBody(
                    "image/*".toMediaTypeOrNull()
                )

                okhttp3.MultipartBody.Part.createFormData("files", fileName, requestBody)
            }.toTypedArray()

            android.util.Log.d("SORA_POST", "Enviando ${parts.size} partes multipart para API")
            val response = apiService.uploadPostMedia(postId, parts)

            if (response.isSuccessful) {
                response.body()?.let { uploadResponse ->
                    android.util.Log.d("SORA_POST", "Upload concluido: ${uploadResponse.media.size} medias uploaded")
                    Result.success(uploadResponse)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                android.util.Log.e("SORA_POST", "Upload falhou: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            android.util.Log.e("SORA_POST", "Excecao no upload: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun likePost(postId: Long): Result<LikeCreateResponse> {
        return try {
            val response = apiService.likePost(postId)
            if (response.isSuccessful) {
                response.body()?.let { likeResponse ->
                    postDao.updatePostLikeStatus(postId, likeResponse.likesCount, true)
                    Result.success(likeResponse)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unlikePost(postId: Long): Result<Unit> {
        return try {
            val response = apiService.unlikePost(postId)
            if (response.isSuccessful) {
                val currentPost = postDao.getPostById(postId)
                val currentCount = currentPost?.likesCount ?: 0
                postDao.updatePostLikeStatus(postId, maxOf(0, currentCount - 1), false)
                Result.success(Unit)
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPostLikes(postId: Long, page: Int, size: Int): Flow<PagingData<LikeModel>> {
        return flow {
            try {
                val response = apiService.getPostLikes(postId, page, size)
                if (response.isSuccessful) {
                    response.body()?.content?.let { likes ->
                        emit(PagingData.from(likes))
                    }
                } else {
                    emit(PagingData.empty())
                }
            } catch (e: Exception) {
                emit(PagingData.empty())
            }
        }
    }

    override suspend fun getPostLikesCount(postId: Long): Flow<Int> {
        return flow {
            val cachedPost = postDao.getPostById(postId)
            val cachedCount = cachedPost?.likesCount ?: 0
            emit(cachedCount)

            try {
                val response = apiService.getPostLikesCount(postId)
                if (response.isSuccessful) {
                    response.body()?.let { countResponse ->
                        postDao.updatePostLikeStatus(postId, countResponse.likesCount, cachedPost?.isLikedByCurrentUser ?: false)
                        emit(countResponse.likesCount)
                    }
                }
            } catch (e: Exception) {
                emit(cachedCount)
            }
        }
    }

    override suspend fun getCachedPosts(): Flow<List<PostModel>> {
        return flow {
            val posts = postDao.getRecentPosts(50)
            emit(posts.map { it.toPostModel() })
        }
    }

    override suspend fun refreshFeed(): Result<Unit> {
        return try {
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCachedUserPosts(userId: Long): Flow<List<PostModel>> {
        return flow {
            val posts = postDao.getRecentPosts(50).filter { it.authorId == userId }
            emit(posts.map { it.toPostModel() })
        }
    }

    private fun isCacheValid(timestamp: Long): Boolean {
        val cacheExpiryMs = 6 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - timestamp) < cacheExpiryMs
    }

    private suspend fun getCurrentUserId(): Long {
        return tokenManager.getUserId() ?: 1L
    }
}

private fun PostModel.toPostEntity(): Post {
    return Post(
        id = id,
        authorId = author.id,
        authorUsername = author.username,
        authorProfilePicture = author.profilePicture,
        profileOwnerId = author.id,
        profileOwnerUsername = author.username,
        countryId = country.id,
        countryCode = country.code,
        collectionId = collection.id,
        collectionCode = collection.code,
        cityName = cityName,
        caption = caption,
        mediaUrls = mediaUrls,
        likesCount = likesCount,
        commentsCount = commentsCount,
        isLikedByCurrentUser = isLikedByCurrentUser,
        visibilityType = visibilityType.name,
        sharedPostGroupId = sharedPostGroupId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        cacheTimestamp = System.currentTimeMillis()
    )
}

private fun Post.toPostModel(): PostModel {
    return PostModel(
        id = id,
        author = UserModel(
            id = authorId,
            username = authorUsername,
            firstName = "",
            lastName = "",
            profilePicture = authorProfilePicture
        ),
        country = CountryModel(
            id = countryId,
            code = countryCode,
            nameKey = ""
        ),
        collection = CollectionModel(
            id = collectionId ?: 0,
            code = collectionCode,
            nameKey = "",
            iconName = null,
            sortOrder = 0,
            isDefault = false
        ),
        cityName = cityName,
        caption = caption,
        media = mediaUrls.map { url ->
            MediaModel(
                id = null,
                fileName = "",
                cloudinaryUrl = url,
                mediaType = MediaType.IMAGE,
                fileSize = null
            )
        },
        likesCount = likesCount,
        commentsCount = commentsCount,
        isLikedByCurrentUser = isLikedByCurrentUser,
        visibilityType = VisibilityType.valueOf(visibilityType),
        sharedPostGroupId = sharedPostGroupId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun GlobePostModel.toPostModel(countryCode: String): PostModel {
    return PostModel(
        id = id,
        author = author,
        country = CountryModel(
            id = 0L,
            code = countryCode,
            nameKey = "",
            latitude = null,
            longitude = null
        ),
        collection = CollectionModel(
            id = 0L,
            code = "GENERAL",
            nameKey = "collection.general",
            iconName = null,
            sortOrder = 1,
            isDefault = true
        ),
        cityName = cityName,
        caption = null,
        media = thumbnailUrl?.let { url ->
            listOf(MediaModel(
                id = null,
                fileName = "",
                cloudinaryUrl = url,
                mediaType = MediaType.IMAGE,
                fileSize = null
            ))
        } ?: emptyList(),
        likesCount = likesCount,
        commentsCount = commentsCount,
        isLikedByCurrentUser = isLikedByCurrentUser,
        createdAt = createdAt,
        updatedAt = null,
        visibilityType = VisibilityType.PERSONAL,
        sharedPostGroupId = null
    )
}