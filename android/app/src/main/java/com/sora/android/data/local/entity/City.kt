package com.sora.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "city")
data class City(
    @PrimaryKey val id: Long,
    val name: String,
    val nameKey: String,
    val latitude: Double,
    val longitude: Double,
    val countryId: Long,
    val countryCode: String,
    val cacheTimestamp: Long = System.currentTimeMillis()
)