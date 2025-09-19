package com.sora.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CityModel(
    val id: Long,
    val name: String,
    val nameKey: String,
    val latitude: Double,
    val longitude: Double,
    val countryId: Long,
    val countryCode: String? = null
)

@Serializable
data class LocationSearchResultModel(
    val query: String,
    val countryFilter: String? = null,
    val results: List<SearchLocationModel>
)

@Serializable
data class SearchLocationModel(
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String? = null,
    val countryName: String? = null,
    val type: String? = null
)

@Serializable
data class ReverseGeocodeResultModel(
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String? = null,
    val countryName: String? = null,
    val cityName: String? = null
)