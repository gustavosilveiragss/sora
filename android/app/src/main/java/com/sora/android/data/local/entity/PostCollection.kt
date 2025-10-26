package com.sora.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collection")
data class PostCollection(
    @PrimaryKey val id: Long,
    val code: String, // GENERAL, CULINARY, EVENTS, OTHERS
    val nameKey: String,
    val iconName: String? = null,
    val sortOrder: Int,
    val isDefault: Boolean = false,
    val cacheTimestamp: Long = System.currentTimeMillis()
)