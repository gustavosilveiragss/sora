package com.sora.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "country")
data class Country(
    @PrimaryKey val id: Long,
    val code: String,
    val nameKey: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    val cacheTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "country_collection")
data class CountryCollection(
    @PrimaryKey val id: String,
    val userId: Long,
    val countryId: Long,
    val countryCode: String,
    val countryNameKey: String,
    val firstVisitDate: String? = null,
    val lastVisitDate: String? = null,
    val visitCount: Int = 0,
    val postsCount: Int = 0,
    val cacheTimestamp: Long = System.currentTimeMillis()
)