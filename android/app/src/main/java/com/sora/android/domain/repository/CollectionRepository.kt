package com.sora.android.domain.repository

import com.sora.android.domain.model.*
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    suspend fun getAllCollections(): Flow<List<CollectionModel>>
    suspend fun getCollectionById(id: Long): Flow<CollectionModel?>
    suspend fun getCollectionByCode(code: String): Flow<CollectionModel?>
    suspend fun getDefaultCollection(): Flow<CollectionModel>

    suspend fun getCachedCollections(): Flow<List<CollectionModel>>
    suspend fun refreshCollections(): Result<Unit>

    suspend fun getCollectionCodes(): Flow<List<String>>
    suspend fun isValidCollectionCode(code: String): Flow<Boolean>
}