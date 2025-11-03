package com.sora.android.domain.repository

import com.sora.android.domain.model.*
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    suspend fun searchLocations(
        query: String,
        countryCode: String? = null,
        limit: Int = 10
    ): Flow<LocationSearchResultModel>

    suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): Flow<ReverseGeocodeResultModel>

    suspend fun searchCitiesInCountry(
        countryCode: String,
        query: String,
        limit: Int = 10
    ): Flow<List<SearchLocationModel>>

    suspend fun getPopularDestinations(
        limit: Int = 10,
        days: Int = 30
    ): Flow<List<CountryModel>>

    suspend fun getCachedLocations(query: String): Flow<List<SearchLocationModel>>
    suspend fun getCachedCities(countryCode: String): Flow<List<CityModel>>
    suspend fun refreshPopularDestinations(): Result<Unit>

    suspend fun validateCoordinates(latitude: Double, longitude: Double): Boolean
    suspend fun getCountryFromCoordinates(latitude: Double, longitude: Double): Flow<CountryModel?>
}