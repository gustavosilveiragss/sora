package com.sora.android.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.sora.android.data.local.entity.TravelPermission
import kotlinx.coroutines.flow.Flow

@Dao
interface TravelPermissionDao {

    @Query("SELECT * FROM travel_permission WHERE grantorId = :userId ORDER BY createdAt DESC")
    fun getGrantedPermissions(userId: Long): PagingSource<Int, TravelPermission>

    @Query("SELECT * FROM travel_permission WHERE grantorId = :userId ORDER BY createdAt DESC")
    suspend fun getGrantedPermissionsAsList(userId: Long): List<TravelPermission>

    @Query("SELECT * FROM travel_permission WHERE granteeId = :userId ORDER BY createdAt DESC")
    fun getReceivedPermissions(userId: Long): PagingSource<Int, TravelPermission>

    @Query("SELECT * FROM travel_permission WHERE granteeId = :userId ORDER BY createdAt DESC")
    suspend fun getReceivedPermissionsAsList(userId: Long): List<TravelPermission>

    @Query("SELECT * FROM travel_permission WHERE grantorId = :userId AND status = :status ORDER BY createdAt DESC")
    fun getGrantedPermissionsByStatus(userId: Long, status: String): PagingSource<Int, TravelPermission>

    @Query("SELECT * FROM travel_permission WHERE granteeId = :userId AND status = :status ORDER BY createdAt DESC")
    fun getReceivedPermissionsByStatus(userId: Long, status: String): PagingSource<Int, TravelPermission>

    @Query("SELECT * FROM travel_permission WHERE id = :permissionId")
    suspend fun getPermissionById(permissionId: Long): TravelPermission?

    @Query("SELECT * FROM travel_permission WHERE grantorId = :grantorId AND granteeId = :granteeId AND countryId = :countryId")
    suspend fun getPermission(grantorId: Long, granteeId: Long, countryId: Long): TravelPermission?

    @Query("SELECT * FROM travel_permission WHERE grantorId = :grantorId AND countryCode = :countryCode AND status = 'ACTIVE'")
    suspend fun getActivePermissionsForCountry(grantorId: Long, countryCode: String): List<TravelPermission>

    @Query("SELECT * FROM travel_permission WHERE granteeId = :granteeId AND countryCode = :countryCode AND status = 'ACTIVE'")
    suspend fun getActivePermissionsAsGrantee(granteeId: Long, countryCode: String): List<TravelPermission>

    @Query("SELECT EXISTS(SELECT 1 FROM travel_permission WHERE grantorId = :grantorId AND granteeId = :granteeId AND countryId = :countryId AND status = 'ACTIVE')")
    suspend fun hasActivePermission(grantorId: Long, granteeId: Long, countryId: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM travel_permission WHERE grantorId = :grantorId AND granteeId = :granteeId AND countryId = :countryId AND status = 'ACTIVE')")
    fun hasActivePermissionFlow(grantorId: Long, granteeId: Long, countryId: Long): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM travel_permission WHERE grantorId = :userId")
    suspend fun getGrantedPermissionsCount(userId: Long): Int

    @Query("SELECT COUNT(*) FROM travel_permission WHERE granteeId = :userId")
    suspend fun getReceivedPermissionsCount(userId: Long): Int

    @Query("SELECT COUNT(*) FROM travel_permission WHERE grantorId = :userId AND status = :status")
    suspend fun getGrantedPermissionsCountByStatus(userId: Long, status: String): Int

    @Query("SELECT COUNT(*) FROM travel_permission WHERE granteeId = :userId AND status = :status")
    suspend fun getReceivedPermissionsCountByStatus(userId: Long, status: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermission(permission: TravelPermission)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermissions(permissions: List<TravelPermission>)

    @Update
    suspend fun updatePermission(permission: TravelPermission)

    @Delete
    suspend fun deletePermission(permission: TravelPermission)

    @Query("DELETE FROM travel_permission WHERE id = :permissionId")
    suspend fun deletePermissionById(permissionId: Long)

    @Query("UPDATE travel_permission SET status = :status, respondedAt = :respondedAt WHERE id = :permissionId")
    suspend fun updatePermissionStatus(permissionId: Long, status: String, respondedAt: String?)

    @Query("DELETE FROM travel_permission WHERE cacheTimestamp < :expiry")
    suspend fun deleteExpiredPermissions(expiry: Long): Int

    @Query("UPDATE travel_permission SET cacheTimestamp = :timestamp WHERE id = :permissionId")
    suspend fun touchPermissionCache(permissionId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM travel_permission")
    suspend fun getPermissionCount(): Int
}