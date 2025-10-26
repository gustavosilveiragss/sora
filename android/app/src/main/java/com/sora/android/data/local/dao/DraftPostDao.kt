package com.sora.android.data.local.dao

import androidx.room.*
import com.sora.android.data.local.entity.DraftPost
import com.sora.android.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftPostDao {

    @Query("SELECT * FROM draft_post ORDER BY createdAt DESC")
    suspend fun getAllDrafts(): List<DraftPost>

    @Query("SELECT * FROM draft_post ORDER BY createdAt DESC")
    fun getAllDraftsFlow(): Flow<List<DraftPost>>

    @Query("SELECT * FROM draft_post WHERE syncStatus = :status ORDER BY createdAt ASC")
    suspend fun getDraftsByStatus(status: SyncStatus): List<DraftPost>

    @Query("SELECT * FROM draft_post WHERE syncStatus = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingDrafts(): List<DraftPost>

    @Query("SELECT * FROM draft_post WHERE syncStatus = 'FAILED' ORDER BY createdAt ASC")
    suspend fun getFailedDrafts(): List<DraftPost>

    @Query("SELECT * FROM draft_post WHERE localId = :localId")
    suspend fun getDraftById(localId: String): DraftPost?

    @Query("SELECT * FROM draft_post WHERE localId = :localId")
    fun getDraftByIdFlow(localId: String): Flow<DraftPost?>

    @Query("SELECT COUNT(*) FROM draft_post")
    suspend fun getDraftCount(): Int

    @Query("SELECT COUNT(*) FROM draft_post WHERE syncStatus = :status")
    suspend fun getDraftCountByStatus(status: SyncStatus): Int

    @Query("SELECT COUNT(*) FROM draft_post WHERE syncStatus = 'PENDING'")
    suspend fun getPendingDraftCount(): Int

    @Query("SELECT COUNT(*) FROM draft_post WHERE syncStatus = 'PENDING'")
    fun getPendingDraftCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: DraftPost)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrafts(drafts: List<DraftPost>)

    @Update
    suspend fun updateDraft(draft: DraftPost)

    @Delete
    suspend fun deleteDraft(draft: DraftPost)

    @Query("DELETE FROM draft_post WHERE localId = :localId")
    suspend fun deleteDraftById(localId: String)

    @Query("UPDATE draft_post SET syncStatus = :status WHERE localId = :localId")
    suspend fun updateDraftStatus(localId: String, status: SyncStatus)

    @Query("DELETE FROM draft_post WHERE syncStatus = 'SYNCED'")
    suspend fun deleteSyncedDrafts()

    @Query("DELETE FROM draft_post WHERE createdAt < :expiry")
    suspend fun deleteExpiredDrafts(expiry: Long): Int

    @Query("DELETE FROM draft_post WHERE syncStatus = 'FAILED' AND createdAt < :expiry")
    suspend fun deleteOldFailedDrafts(expiry: Long): Int

    @Transaction
    suspend fun markDraftAsSynced(localId: String) {
        updateDraftStatus(localId, SyncStatus.SYNCED)
    }

    @Transaction
    suspend fun markDraftAsFailed(localId: String) {
        updateDraftStatus(localId, SyncStatus.FAILED)
    }

    @Transaction
    suspend fun markDraftAsUploading(localId: String) {
        updateDraftStatus(localId, SyncStatus.UPLOADING)
    }
}