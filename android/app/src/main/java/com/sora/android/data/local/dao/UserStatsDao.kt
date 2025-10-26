package com.sora.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sora.android.data.local.entity.CachedUserStats
import kotlinx.coroutines.flow.Flow

@Dao
interface   UserStatsDao {

    @Query("SELECT * FROM cached_user_stats WHERE userId = :userId")
    suspend fun getUserStats(userId: Long): CachedUserStats?

    @Query("SELECT * FROM cached_user_stats WHERE userId = :userId")
    fun getUserStatsFlow(userId: Long): Flow<CachedUserStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserStats(stats: CachedUserStats)

    @Query("DELETE FROM cached_user_stats WHERE userId = :userId")
    suspend fun deleteUserStats(userId: Long)

    @Query("DELETE FROM cached_user_stats WHERE cacheTimestamp < :expiry")
    suspend fun deleteExpiredStats(expiry: Long): Int

    @Query("SELECT COUNT(*) FROM cached_user_stats")
    suspend fun getStatsCount(): Int
}
