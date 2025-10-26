package com.sora.android.data.repository

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.sora.android.data.local.TokenManager
import com.sora.android.data.local.dao.TravelPermissionDao
import com.sora.android.data.local.entity.TravelPermission
import com.sora.android.data.remote.ApiService
import com.sora.android.data.remote.util.NetworkUtils
import com.sora.android.domain.model.*
import com.sora.android.domain.repository.TravelPermissionRepository
import com.sora.android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TravelPermissionRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val travelPermissionDao: TravelPermissionDao,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : TravelPermissionRepository {

    override suspend fun grantTravelPermission(request: TravelPermissionRequest): Result<TravelPermissionModel> {
        return try {
            val response = apiService.grantTravelPermission(request)
            if (response.isSuccessful) {
                response.body()?.let { permission ->
                    val permissionEntity = permission.toTravelPermissionEntity()
                    travelPermissionDao.insertPermission(permissionEntity)
                    Result.success(permission)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun acceptPermission(permissionId: Long): Result<TravelPermissionModel> {
        return try {
            val response = apiService.acceptTravelPermission(permissionId)
            if (response.isSuccessful) {
                response.body()?.let { permission ->
                    val permissionEntity = permission.toTravelPermissionEntity()
                    travelPermissionDao.updatePermission(permissionEntity)
                    Result.success(permission)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun declinePermission(permissionId: Long): Result<TravelPermissionModel> {
        return try {
            val response = apiService.declineTravelPermission(permissionId)
            if (response.isSuccessful) {
                response.body()?.let { permission ->
                    val permissionEntity = permission.toTravelPermissionEntity()
                    travelPermissionDao.updatePermission(permissionEntity)
                    Result.success(permission)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun revokePermission(permissionId: Long): Result<Unit> {
        return try {
            val response = apiService.revokeTravelPermission(permissionId)
            if (response.isSuccessful) {
                travelPermissionDao.deletePermissionById(permissionId)
                Result.success(Unit)
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGrantedPermissions(
        status: String?,
        page: Int,
        size: Int
    ): Flow<PagingData<TravelPermissionModel>> {
        return flow {
            try {
                val response = apiService.getGrantedPermissions(status, page, size)
                if (response.isSuccessful) {
                    response.body()?.content?.let { permissions ->
                        val entities = permissions.map { it.toTravelPermissionEntity() }
                        travelPermissionDao.insertPermissions(entities)
                        emit(PagingData.from(permissions))
                    }
                } else {
                    emitCachedGrantedPermissions(status)
                }
            } catch (e: Exception) {
                emitCachedGrantedPermissions(status)
            }
        }
    }

    override suspend fun getReceivedPermissions(
        status: String?,
        page: Int,
        size: Int
    ): Flow<PagingData<TravelPermissionModel>> {
        return flow {
            try {
                val response = apiService.getReceivedPermissions(status, page, size)
                if (response.isSuccessful) {
                    response.body()?.content?.let { permissions ->
                        val entities = permissions.map { it.toTravelPermissionEntity() }
                        travelPermissionDao.insertPermissions(entities)
                        emit(PagingData.from(permissions))
                    }
                } else {
                    emitCachedReceivedPermissions(status)
                }
            } catch (e: Exception) {
                emitCachedReceivedPermissions(status)
            }
        }
    }

    override suspend fun hasPermissionForCountry(userId: Long, countryCode: String): Flow<Boolean> {
        return flow {
            val permissions = travelPermissionDao.getActivePermissionsAsGrantee(userId, countryCode)
            emit(permissions.isNotEmpty())
        }
    }

    override suspend fun getPermissionForCountry(userId: Long, countryCode: String): Flow<TravelPermissionModel?> {
        return flow {
            val permissions = travelPermissionDao.getActivePermissionsAsGrantee(userId, countryCode)
            val permission = permissions.firstOrNull()
            emit(permission?.toTravelPermissionModel())
        }
    }

    override suspend fun getCachedGrantedPermissions(): Flow<List<TravelPermissionModel>> {
        return flow {
            val userId = getCurrentUserId()
            val permissions = travelPermissionDao.getGrantedPermissionsAsList(userId)
            emit(permissions.map { it.toTravelPermissionModel() })
        }
    }

    override suspend fun getCachedReceivedPermissions(): Flow<List<TravelPermissionModel>> {
        return flow {
            val userId = getCurrentUserId()
            val permissions = travelPermissionDao.getReceivedPermissionsAsList(userId)
            emit(permissions.map { it.toTravelPermissionModel() })
        }
    }

    override suspend fun refreshPermissions(): Result<Unit> {
        return try {
            val grantedResponse = apiService.getGrantedPermissions(null, 0, 100)
            val receivedResponse = apiService.getReceivedPermissions(null, 0, 100)

            if (grantedResponse.isSuccessful && receivedResponse.isSuccessful) {
                val grantedPermissions = grantedResponse.body()?.content?.map { it.toTravelPermissionEntity() } ?: emptyList()
                val receivedPermissions = receivedResponse.body()?.content?.map { it.toTravelPermissionEntity() } ?: emptyList()

                val allPermissions = grantedPermissions + receivedPermissions
                travelPermissionDao.insertPermissions(allPermissions)

                Result.success(Unit)
            } else {
                Result.failure(Exception(context.getString(R.string.error_sync_permissions_failed)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActivePermissionsCount(): Flow<Int> {
        return flow {
            val userId = getCurrentUserId()
            val count = travelPermissionDao.getGrantedPermissionsCountByStatus(userId, "ACTIVE")
            emit(count)
        }
    }

    override suspend fun getPendingPermissionsCount(): Flow<Int> {
        return flow {
            val userId = getCurrentUserId()
            val count = travelPermissionDao.getReceivedPermissionsCountByStatus(userId, "PENDING")
            emit(count)
        }
    }

    private suspend fun FlowCollector<PagingData<TravelPermissionModel>>.emitCachedGrantedPermissions(status: String?) {
        val userId = getCurrentUserId()
        val pagingSource = if (status != null) {
            travelPermissionDao.getGrantedPermissionsByStatus(userId, status)
        } else {
            travelPermissionDao.getGrantedPermissions(userId)
        }

        val pager = Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { pagingSource }
        )

        pager.flow.map { pagingData ->
            pagingData.map { it.toTravelPermissionModel() }
        }.collect { emit(it) }
    }

    private suspend fun FlowCollector<PagingData<TravelPermissionModel>>.emitCachedReceivedPermissions(status: String?) {
        val userId = getCurrentUserId()
        val pagingSource = if (status != null) {
            travelPermissionDao.getReceivedPermissionsByStatus(userId, status)
        } else {
            travelPermissionDao.getReceivedPermissions(userId)
        }

        val pager = Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { pagingSource }
        )

        pager.flow.map { pagingData ->
            pagingData.map { it.toTravelPermissionModel() }
        }.collect { emit(it) }
    }

    private fun isCacheValid(timestamp: Long): Boolean {
        val cacheExpiryMs = 12 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - timestamp) < cacheExpiryMs
    }

    private suspend fun getCurrentUserId(): Long {
        return tokenManager.getUserId() ?: 1L
    }
}

private fun TravelPermission.toTravelPermissionModel(): TravelPermissionModel {
    return TravelPermissionModel(
        id = id,
        grantor = UserModel(
            id = grantorId,
            username = grantorUsername,
            firstName = "",
            lastName = "",
            profilePicture = null
        ),
        grantee = UserModel(
            id = granteeId,
            username = granteeUsername,
            firstName = "",
            lastName = "",
            profilePicture = null
        ),
        country = CountryModel(
            id = countryId,
            code = countryCode,
            nameKey = countryNameKey
        ),
        status = PermissionStatus.valueOf(status),
        invitationMessage = invitationMessage,
        createdAt = createdAt,
        respondedAt = respondedAt
    )
}

private fun TravelPermissionModel.toTravelPermissionEntity(): TravelPermission {
    return TravelPermission(
        id = id,
        grantorId = grantor.id,
        grantorUsername = grantor.username,
        granteeId = grantee.id,
        granteeUsername = grantee.username,
        countryId = country.id,
        countryCode = country.code,
        countryNameKey = country.nameKey,
        status = status.name,
        invitationMessage = invitationMessage,
        respondedAt = respondedAt,
        createdAt = createdAt,
        cacheTimestamp = System.currentTimeMillis()
    )
}