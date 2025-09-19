package com.sora.android.domain.repository

import androidx.paging.PagingData
import com.sora.android.data.remote.dto.CountryCollectionsResponse
import com.sora.android.data.remote.dto.CountryPostsResponse
import com.sora.android.domain.model.*
import kotlinx.coroutines.flow.Flow

interface CountryRepository {
    suspend fun getMyCountryCollections(): Flow<CountryCollectionsResponse>
    suspend fun getUserCountryCollections(userId: Long): Flow<CountryCollectionsResponse>

    suspend fun getCountryPosts(
        userId: Long,
        countryCode: String,
        collectionCode: String? = null,
        cityName: String? = null,
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "createdAt",
        sortDirection: String = "DESC"
    ): Flow<CountryPostsResponse>

    suspend fun getAllCountries(): Flow<List<CountryModel>>
    suspend fun getCountryByCode(countryCode: String): Flow<CountryModel?>
    suspend fun searchCountries(query: String, limit: Int = 10): Flow<List<CountryModel>>

    suspend fun getCachedCountries(): Flow<List<CountryModel>>
    suspend fun getCachedCountryCollections(userId: Long): Flow<List<CountryCollectionModel>>
    suspend fun refreshCountryCollections(): Result<Unit>
    suspend fun refreshCountriesData(): Result<Unit>

    suspend fun getCountriesVisitedCount(userId: Long): Flow<Int>
    suspend fun getTotalCitiesVisited(userId: Long): Flow<Int>
    suspend fun getTotalPostsCount(userId: Long): Flow<Int>
}