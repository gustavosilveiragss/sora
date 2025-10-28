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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val postDao: PostDao,
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
                com.sora.android.data.paging.FeedPagingSource(apiService)
            }
        ).flow
    }

    override suspend fun getUserPosts(userId: Long, page: Int, size: Int): Flow<PagingData<PostModel>> {
        return flow {
            android.util.Log.d("PostRepository", "getUserPosts called for userId: $userId")
            val cachedPosts = postDao.getRecentPosts(size).filter { it.authorId == userId }

            if (cachedPosts.isNotEmpty()) {
                android.util.Log.d("PostRepository", "Emitting ${cachedPosts.size} cached posts")
                emit(PagingData.from(cachedPosts.map { it.toPostModel() }))
            }

            try {
                android.util.Log.d("PostRepository", "Fetching country collections from API...")
                val countriesResponse = apiService.getUserCountryCollections(userId)
                android.util.Log.d("PostRepository", "Country collections response: ${countriesResponse.code()}")
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
                                    val postEntities = posts.map { it.toPostEntity() }
                                    postEntities.forEach { postDao.insertPost(it) }
                                }
                            }
                        }

                        if (allPosts.isNotEmpty()) {
                            android.util.Log.d("PostRepository", "Emitting ${allPosts.size} posts from API")
                            emit(PagingData.from(allPosts))
                        } else if (cachedPosts.isEmpty()) {
                            android.util.Log.d("PostRepository", "No posts found, emitting empty")
                            emit(PagingData.empty())
                        }
                    }
                } else if (cachedPosts.isEmpty()) {
                    android.util.Log.w("PostRepository", "API error and no cache, emitting empty")
                    emit(PagingData.empty())
                }
            } catch (e: Exception) {
                android.util.Log.e("PostRepository", "Error loading posts", e)
                if (cachedPosts.isEmpty()) {
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
        return flow {
            val cachedPosts = postDao.getRecentPosts(50)
                .filter { it.countryCode == countryCode && it.profileOwnerId == userId }
                .let { posts ->
                    if (collectionCode != null) {
                        posts.filter { it.collectionCode == collectionCode }
                    } else posts
                }
                .let { posts ->
                    if (cityName != null) {
                        posts.filter { it.cityName == cityName }
                    } else posts
                }

            if (cachedPosts.isNotEmpty() && isCacheValid(cachedPosts.first().cacheTimestamp)) {
                emit(PagingData.from(cachedPosts.map { it.toPostModel() }))
            }

            if (!networkMonitor.isCurrentlyConnected()) {
                if (cachedPosts.isNotEmpty()) {
                    emit(PagingData.from(cachedPosts.map { it.toPostModel() }))
                } else {
                    emit(PagingData.empty())
                }
                return@flow
            }

            try {
                val response = apiService.getCountryPosts(
                    userId, countryCode, collectionCode, cityName, page, size, sortBy, sortDirection
                )
                if (response.isSuccessful) {
                    val posts = response.body()?.posts?.content ?: emptyList()
                    if (posts.isNotEmpty()) {
                        val postEntities = posts.map { it.toPostEntity() }
                        postEntities.forEach { postDao.insertPost(it) }
                    }
                    emit(PagingData.from(posts))
                } else if (cachedPosts.isNotEmpty()) {
                    emit(PagingData.from(cachedPosts.map { it.toPostModel() }))
                } else {
                    emit(PagingData.empty())
                }
            } catch (e: Exception) {
                if (cachedPosts.isNotEmpty()) {
                    emit(PagingData.from(cachedPosts.map { it.toPostModel() }))
                } else {
                    emit(PagingData.empty())
                }
            }
        }
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
            Result.failure(Exception(context.getString(R.string.error_media_upload_not_implemented)))
        } catch (e: Exception) {
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