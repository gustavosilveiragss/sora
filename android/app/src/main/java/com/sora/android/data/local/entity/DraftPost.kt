package com.sora.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.sora.android.data.local.converter.StringListConverter

@Entity(tableName = "draft_post")
@TypeConverters(StringListConverter::class)
data class DraftPost(
    @PrimaryKey val localId: String,
    val countryCode: String,
    val collectionCode: String,
    val cityName: String,
    val cityLatitude: Double,
    val cityLongitude: Double,
    val caption: String? = null,
    val localImageUris: List<String> = emptyList(),
    val sharingOption: String = "PERSONAL_ONLY",
    val profileOwnerId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

enum class SyncStatus {
    PENDING, UPLOADING, SYNCED, FAILED
}