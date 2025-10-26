package com.sora.android.domain.repository

import androidx.paging.PagingData
import com.sora.android.data.remote.dto.ProfilePictureResponse
import com.sora.android.data.remote.dto.UpdateProfileRequest
import com.sora.android.domain.model.*
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getCurrentUserProfile(): Result<UserModel>
    suspend fun getUserProfile(userId: Long): Flow<UserProfileModel?>
    suspend fun updateUserProfile(request: UpdateProfileRequest): Result<UserProfileModel>
    suspend fun uploadProfilePicture(imageUri: String): Result<ProfilePictureResponse>
    suspend fun getUserTravelStats(userId: Long): Result<TravelStatsModel>
    suspend fun getUserRankings(userId: Long): Result<UserRankingsModel>
    suspend fun getRecentDestinations(userId: Long, limit: Int = 10): Result<List<RecentDestinationModel>>

    suspend fun searchUsers(
        query: String,
        countryCode: String? = null,
        page: Int = 0,
        size: Int = 10
    ): Flow<PagingData<UserSearchResultModel>>

    suspend fun followUser(userId: Long): Result<FollowModel>
    suspend fun unfollowUser(userId: Long): Result<Unit>
    suspend fun getUserFollowers(userId: Long, page: Int = 0, size: Int = 20): Flow<PagingData<UserModel>>
    suspend fun getUserFollowing(userId: Long, page: Int = 0, size: Int = 20): Flow<PagingData<UserModel>>

    suspend fun getUserFollowersList(userId: Long, page: Int = 0, size: Int = 20): Result<List<UserModel>>
    suspend fun getUserFollowingList(userId: Long, page: Int = 0, size: Int = 20): Result<List<UserModel>>

    suspend fun getCachedUser(userId: Long): Flow<UserModel?>
    suspend fun getCachedUsers(): Flow<List<UserModel>>
    suspend fun refreshUserProfile(userId: Long): Result<Unit>
    suspend fun isCachedUserFollowed(userId: Long): Flow<Boolean>
}