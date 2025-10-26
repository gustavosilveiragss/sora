package com.sora.android.data.local.dao

import androidx.room.*
import com.sora.android.data.local.entity.PostCollection
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collection ORDER BY sortOrder ASC, nameKey ASC")
    suspend fun getAllCollections(): List<PostCollection>

    @Query("SELECT * FROM collection ORDER BY sortOrder ASC, nameKey ASC")
    fun getAllCollectionsFlow(): Flow<List<PostCollection>>

    @Query("SELECT * FROM collection WHERE id = :collectionId")
    suspend fun getCollectionById(collectionId: Long): PostCollection?

    @Query("SELECT * FROM collection WHERE code = :code")
    suspend fun getCollectionByCode(code: String): PostCollection?

    @Query("SELECT * FROM collection WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultCollection(): PostCollection?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: PostCollection)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollections(collections: List<PostCollection>)

    @Update
    suspend fun updateCollection(collection: PostCollection)

    @Delete
    suspend fun deleteCollection(collection: PostCollection)

    @Query("DELETE FROM collection WHERE cacheTimestamp < :expiry")
    suspend fun deleteExpiredCollections(expiry: Long): Int

    @Query("UPDATE collection SET cacheTimestamp = :timestamp WHERE id = :collectionId")
    suspend fun touchCollectionCache(collectionId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM collection")
    suspend fun getCollectionCount(): Int
}