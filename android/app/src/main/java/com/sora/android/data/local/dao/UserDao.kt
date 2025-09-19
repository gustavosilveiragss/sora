package com.sora.android.data.local.dao

import androidx.room.*
import com.sora.android.data.local.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM user ORDER BY username ASC")
    suspend fun getAllUsers(): List<User>

    @Query("SELECT * FROM user WHERE id = :userId")
    suspend fun getUserById(userId: Long): User?

    @Query("SELECT * FROM user WHERE id = :userId")
    fun getUserByIdFlow(userId: Long): Flow<User?>

    @Query("SELECT * FROM user WHERE username LIKE '%' || :query || '%' ORDER BY username ASC LIMIT :limit")
    suspend fun searchUsers(query: String, limit: Int = 10): List<User>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("DELETE FROM user WHERE cacheTimestamp < :expiry")
    suspend fun deleteExpiredUsers(expiry: Long): Int

    @Query("DELETE FROM user WHERE id NOT IN (SELECT id FROM user ORDER BY cacheTimestamp DESC LIMIT :keepCount)")
    suspend fun deleteOldestUsersExceedingLimit(keepCount: Int)

    @Query("UPDATE user SET cacheTimestamp = :timestamp WHERE id = :userId")
    suspend fun touchUserCache(userId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM user")
    suspend fun getUserCount(): Int
}