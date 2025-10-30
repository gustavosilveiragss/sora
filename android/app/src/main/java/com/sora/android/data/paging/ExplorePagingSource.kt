package com.sora.android.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sora.android.core.network.NetworkMonitor
import com.sora.android.data.local.dao.PostDao
import com.sora.android.data.local.dao.UserDao
import com.sora.android.data.local.entity.Post
import com.sora.android.data.local.entity.User
import com.sora.android.data.remote.ApiService
import com.sora.android.domain.model.*

class ExplorePagingSource(
    private val apiService: ApiService,
    private val postDao: PostDao,
    private val userDao: UserDao,
    private val networkMonitor: NetworkMonitor,
    private val timeframe: String
) : PagingSource<Int, PostModel>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PostModel> {
        val page = params.key ?: 0

        return try {
            if (!networkMonitor.isCurrentlyConnected()) {
                Log.d("ExplorePagingSource", "OFFLINE MODE: timeframe=$timeframe, loading from cache")

                val cachedPosts = when (timeframe) {
                    "week" -> {
                        val weekAgo = calculateDateDaysAgo(7)
                        Log.d("ExplorePagingSource", "Loading posts since $weekAgo (7 days ago)")
                        postDao.getPostsByRelevanceSince(weekAgo, params.loadSize)
                    }
                    "month" -> {
                        val monthAgo = calculateDateDaysAgo(30)
                        Log.d("ExplorePagingSource", "Loading posts since $monthAgo (30 days ago)")
                        postDao.getPostsByRelevanceSince(monthAgo, params.loadSize)
                    }
                    else -> {
                        Log.d("ExplorePagingSource", "Loading all posts by relevance")
                        postDao.getPostsByRelevance(params.loadSize)
                    }
                }

                Log.d("ExplorePagingSource", "CACHE RESULT: Found ${cachedPosts.size} posts")

                val posts = cachedPosts.map { it.toPostModel() }

                return LoadResult.Page(
                    data = posts,
                    prevKey = null,
                    nextKey = null
                )
            }

            val response = apiService.getExplorePosts(
                timeframe = timeframe,
                page = page,
                size = params.loadSize
            )

            if (response.isSuccessful) {
                val pagedResponse = response.body()
                val posts = pagedResponse?.content ?: emptyList()
                val isLastPage = pagedResponse?.last ?: true

                posts.forEach { post ->
                    val existingUser = userDao.getUserById(post.author.id)
                    val userEntity = User(
                        id = post.author.id,
                        username = post.author.username,
                        firstName = post.author.firstName,
                        lastName = post.author.lastName,
                        bio = post.author.bio ?: existingUser?.bio,
                        profilePicture = post.author.profilePicture ?: existingUser?.profilePicture,
                        followersCount = post.author.followersCount ?: 0,
                        followingCount = post.author.followingCount ?: 0,
                        countriesVisitedCount = post.author.countriesVisitedCount ?: 0,
                        cacheTimestamp = System.currentTimeMillis()
                    )
                    userDao.insertUser(userEntity)
                }

                val postEntities = posts.map { it.toPostEntity() }
                postDao.insertPosts(postEntities)

                LoadResult.Page(
                    data = posts,
                    prevKey = if (page == 0) null else page - 1,
                    nextKey = if (isLastPage) null else page + 1
                )
            } else {
                Log.d("ExplorePagingSource", "API FAILED: fallback to cache with timeframe=$timeframe")

                val cachedPosts = when (timeframe) {
                    "week" -> postDao.getPostsByRelevanceSince(calculateDateDaysAgo(7), params.loadSize)
                    "month" -> postDao.getPostsByRelevanceSince(calculateDateDaysAgo(30), params.loadSize)
                    else -> postDao.getPostsByRelevance(params.loadSize)
                }

                Log.d("ExplorePagingSource", "CACHE FALLBACK: Found ${cachedPosts.size} posts")

                LoadResult.Page(
                    data = cachedPosts.map { it.toPostModel() },
                    prevKey = null,
                    nextKey = null
                )
            }
        } catch (e: Exception) {
            Log.d("ExplorePagingSource", "EXCEPTION: fallback to cache, error: ${e.message}")

            val cachedPosts = when (timeframe) {
                "week" -> postDao.getPostsByRelevanceSince(calculateDateDaysAgo(7), params.loadSize)
                "month" -> postDao.getPostsByRelevanceSince(calculateDateDaysAgo(30), params.loadSize)
                else -> postDao.getPostsByRelevance(params.loadSize)
            }

            Log.d("ExplorePagingSource", "EXCEPTION FALLBACK: Found ${cachedPosts.size} posts")

            return LoadResult.Page(
                data = cachedPosts.map { it.toPostModel() },
                prevKey = null,
                nextKey = null
            )
        }
    }

    private fun calculateDateDaysAgo(days: Int): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -days)
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        return dateFormat.format(calendar.time)
    }

    override fun getRefreshKey(state: PagingState<Int, PostModel>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
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
