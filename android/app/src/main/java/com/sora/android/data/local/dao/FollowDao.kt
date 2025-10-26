package com.sora.android.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.sora.android.data.local.entity.Follow
import com.sora.android.data.local.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowDao {

    @Query("SELECT * FROM follow WHERE followerId = :userId ORDER BY createdAt DESC")
    suspend fun getUserFollowing(userId: Long): List<Follow>

    @Query("SELECT * FROM follow WHERE followingId = :userId ORDER BY createdAt DESC")
    suspend fun getUserFollowers(userId: Long): List<Follow>

    @Query("SELECT u.* FROM user u INNER JOIN follow f ON u.id = f.followingId WHERE f.followerId = :userId ORDER BY f.createdAt DESC")
    fun getFollowingUsers(userId: Long): PagingSource<Int, User>

    @Query("SELECT u.* FROM user u INNER JOIN follow f ON u.id = f.followerId WHERE f.followingId = :userId ORDER BY f.createdAt DESC")
    fun getFollowerUsers(userId: Long): PagingSource<Int, User>

    @Query("SELECT * FROM follow WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun getFollow(followerId: Long, followingId: Long): Follow?

    @Query("SELECT EXISTS(SELECT 1 FROM follow WHERE followerId = :followerId AND followingId = :followingId)")
    suspend fun isFollowing(followerId: Long, followingId: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM follow WHERE followerId = :followerId AND followingId = :followingId)")
    fun isFollowingFlow(followerId: Long, followingId: Long): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM follow WHERE followerId = :userId")
    suspend fun getFollowingCount(userId: Long): Int

    @Query("SELECT COUNT(*) FROM follow WHERE followingId = :userId")
    suspend fun getFollowersCount(userId: Long): Int

    @Query("SELECT COUNT(*) FROM follow WHERE followerId = :userId")
    fun getFollowingCountFlow(userId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM follow WHERE followingId = :userId")
    fun getFollowersCountFlow(userId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: Follow)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollows(follows: List<Follow>)

    @Delete
    suspend fun deleteFollow(follow: Follow)

    @Query("DELETE FROM follow WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun deleteFollow(followerId: Long, followingId: Long)

    @Query("DELETE FROM follow WHERE cacheTimestamp < :expiry")
    suspend fun deleteExpiredFollows(expiry: Long): Int

    @Query("UPDATE follow SET cacheTimestamp = :timestamp WHERE id = :followId")
    suspend fun touchFollowCache(followId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM follow")
    suspend fun getFollowCount(): Int
}