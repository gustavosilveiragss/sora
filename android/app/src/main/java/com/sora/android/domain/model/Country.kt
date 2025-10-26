package com.sora.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CountryModel(
    val id: Long,
    val code: String,
    val nameKey: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null
)

@Serializable
data class CountryCollectionModel(
    val countryId: Long,
    val countryCode: String,
    val countryNameKey: String,
    val firstVisitDate: String? = null,
    val lastVisitDate: String? = null,
    val visitCount: Int = 0,
    val postsCount: Int = 0,
    val latestPostImageUrl: String? = null
)

@Serializable
data class CountryVisitInfoModel(
    val firstVisitDate: String? = null,
    val lastVisitDate: String? = null,
    val visitCount: Int = 0,
    val totalPostsCount: Int = 0,
    val citiesVisited: List<String>? = null
)