package com.sora.android.domain.repository

import androidx.paging.PagingData
import com.sora.android.data.remote.dto.LikeCreateResponse
import com.sora.android.data.remote.dto.MediaUploadResponse
import com.sora.android.domain.model.*
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    suspend fun getPostById(id: Long): Flow<PostModel?>
    suspend fun createPost(request: PostCreateRequest): Result<List<PostModel>>
    suspend fun updatePost(postId: Long, request: PostUpdateRequest): Result<PostModel>
    suspend fun deletePost(postId: Long): Result<Unit>

    suspend fun getFeed(page: Int = 0, size: Int = 20): Flow<PagingData<PostModel>>
    suspend fun getUserPosts(userId: Long, page: Int = 0, size: Int = 20): Flow<PagingData<PostModel>>
    suspend fun getCountryPosts(
        userId: Long,
        countryCode: String,
        collectionCode: String? = null,
        cityName: String? = null,
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "createdAt",
        sortDirection: String = "DESC"
    ): Flow<PagingData<PostModel>>

    suspend fun getSharedPostGroup(groupId: String): Flow<List<PostModel>>

    suspend fun uploadPostMedia(postId: Long, imageUris: List<String>): Result<MediaUploadResponse>

    suspend fun likePost(postId: Long): Result<LikeCreateResponse>
    suspend fun unlikePost(postId: Long): Result<Unit>
    suspend fun getPostLikes(postId: Long, page: Int = 0, size: Int = 20): Flow<PagingData<LikeModel>>
    suspend fun getPostLikesCount(postId: Long): Flow<Int>

    suspend fun getCachedPosts(): Flow<List<PostModel>>
    suspend fun refreshFeed(): Result<Unit>
    suspend fun getCachedUserPosts(userId: Long): Flow<List<PostModel>>
}