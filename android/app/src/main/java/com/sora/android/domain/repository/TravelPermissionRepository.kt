package com.sora.android.domain.repository

import androidx.paging.PagingData
import com.sora.android.domain.model.*
import kotlinx.coroutines.flow.Flow

interface TravelPermissionRepository {
    suspend fun grantTravelPermission(request: TravelPermissionRequest): Result<TravelPermissionModel>
    suspend fun acceptPermission(permissionId: Long): Result<TravelPermissionModel>
    suspend fun declinePermission(permissionId: Long): Result<TravelPermissionModel>
    suspend fun revokePermission(permissionId: Long): Result<Unit>

    suspend fun getGrantedPermissions(
        status: String? = null,
        page: Int = 0,
        size: Int = 20
    ): Flow<PagingData<TravelPermissionModel>>

    suspend fun getReceivedPermissions(
        status: String? = null,
        page: Int = 0,
        size: Int = 20
    ): Flow<PagingData<TravelPermissionModel>>

    suspend fun hasPermissionForCountry(userId: Long, countryCode: String): Flow<Boolean>
    suspend fun getPermissionForCountry(userId: Long, countryCode: String): Flow<TravelPermissionModel?>

    suspend fun getCachedGrantedPermissions(): Flow<List<TravelPermissionModel>>
    suspend fun getCachedReceivedPermissions(): Flow<List<TravelPermissionModel>>
    suspend fun refreshPermissions(): Result<Unit>

    suspend fun getActivePermissionsCount(): Flow<Int>
    suspend fun getPendingPermissionsCount(): Flow<Int>
}