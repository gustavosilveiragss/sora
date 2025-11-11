package com.sora.android.data.repository

import android.content.Context
import android.util.Log
import com.sora.android.data.local.TokenManager
import com.sora.android.data.local.dao.CountryDao
import com.sora.android.data.local.dao.CityDao
import com.sora.android.data.local.entity.Country
import com.sora.android.data.local.entity.CountryCollection
import com.sora.android.data.remote.ApiService
import com.sora.android.data.remote.util.NetworkUtils
import com.sora.android.data.remote.dto.CountryCollectionsResponse
import com.sora.android.data.remote.dto.CountryPostsResponse
import com.sora.android.domain.model.*
import com.sora.android.domain.repository.CountryRepository
import com.sora.android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CountryRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val countryDao: CountryDao,
    private val cityDao: CityDao,
    private val tokenManager: TokenManager,
    private val networkMonitor: com.sora.android.core.network.NetworkMonitor,
    @ApplicationContext private val context: Context
) : CountryRepository {

    override suspend fun getMyCountryCollections(): Flow<CountryCollectionsResponse> {
        val currentUserId = tokenManager.getUserId() ?: 1L
        return offlineFirstData(
            tag = "CountryCollections-$currentUserId",
            networkMonitor = networkMonitor,
            getCached = {
                val collections = countryDao.getCountryCollectionsByUser(currentUserId)
                CountryCollectionsResponse(
                    userId = currentUserId,
                    username = "",
                    totalCountriesVisited = collections.size,
                    totalCitiesVisited = 0,
                    totalPostsCount = collections.sumOf { it.postsCount },
                    countries = collections.map { it.toCountryCollectionModel() }
                )
            },
            fetchFromApi = {
                val response = apiService.getCurrentUserCountryCollections()
                if (response.isSuccessful) {
                    response.body()?.also { collections ->
                        val entities = collections.countries.map { model ->
                            CountryCollection(
                                id = "${collections.userId}_${model.countryCode}",
                                userId = collections.userId,
                                countryId = model.countryId,
                                countryCode = model.countryCode,
                                countryNameKey = model.countryNameKey,
                                firstVisitDate = model.firstVisitDate,
                                lastVisitDate = model.lastVisitDate,
                                visitCount = model.visitCount,
                                postsCount = model.postsCount,
                                cacheTimestamp = System.currentTimeMillis()
                            )
                        }
                        countryDao.insertCountryCollections(entities)
                    }
                } else null
            }
        )
    }

    override suspend fun getUserCountryCollections(userId: Long): Flow<CountryCollectionsResponse> {
        return offlineFirstData(
            tag = "CountryCollections-$userId",
            networkMonitor = networkMonitor,
            getCached = {
                val collections = countryDao.getCountryCollectionsByUser(userId)
                CountryCollectionsResponse(
                    userId = userId,
                    username = "",
                    totalCountriesVisited = collections.size,
                    totalCitiesVisited = 0,
                    totalPostsCount = collections.sumOf { it.postsCount },
                    countries = collections.map { it.toCountryCollectionModel() }
                )
            },
            fetchFromApi = {
                val response = apiService.getUserCountryCollections(userId)
                if (response.isSuccessful) {
                    response.body()?.also { collections ->
                        val entities = collections.countries.map { model ->
                            CountryCollection(
                                id = "${userId}_${model.countryCode}",
                                userId = userId,
                                countryId = model.countryId,
                                countryCode = model.countryCode,
                                countryNameKey = model.countryNameKey,
                                firstVisitDate = model.firstVisitDate,
                                lastVisitDate = model.lastVisitDate,
                                visitCount = model.visitCount,
                                postsCount = model.postsCount,
                                cacheTimestamp = System.currentTimeMillis()
                            )
                        }
                        countryDao.insertCountryCollections(entities)
                    }
                } else null
            }
        )
    }

    override suspend fun getCountryPosts(
        userId: Long,
        countryCode: String,
        collectionCode: String?,
        cityName: String?,
        timeframe: String,
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): Flow<CountryPostsResponse> {
        return flow {
            try {
                val response = apiService.getCountryPosts(
                    userId, countryCode, collectionCode, cityName, page, size, sortBy, sortDirection
                )
                if (response.isSuccessful) {
                    response.body()?.let { posts ->
                        emit(posts)
                    }
                }
            } catch (e: Exception) {
                Log.e("CountryRepository", "Error getting country posts: ${e.message}", e)
            }
        }
    }

    override suspend fun getAllCountries(): Flow<List<CountryModel>> {
        return flow {
            try {
                val cachedCountries = countryDao.getAllCountries()
                if (cachedCountries.isNotEmpty()) {
                    emit(cachedCountries.map { it.toCountryModel() })
                }

                Log.d("CountryRepository", "Fetching countries from API...")
                val response = apiService.getAllCountries()
                if (response.isSuccessful) {
                    response.body()?.let { countries ->
                        Log.d("CountryRepository", "Received ${countries.size} countries from API")

                        val countryEntities = countries.map { it.toCountryEntity() }
                        countryDao.insertCountries(countryEntities)

                        emit(countries)
                    }
                } else {
                    Log.e("CountryRepository", "API Error: ${response.code()}")
                    if (cachedCountries.isEmpty()) {
                        emit(emptyList())
                    }
                }
            } catch (e: Exception) {
                Log.e("CountryRepository", "Error fetching countries: ${e.message}", e)

                val cachedCountries = countryDao.getAllCountries()
                if (cachedCountries.isNotEmpty()) {
                    emit(cachedCountries.map { it.toCountryModel() })
                } else {
                    emit(emptyList())
                }
            }
        }
    }

    override suspend fun getCountryByCode(countryCode: String): Flow<CountryModel?> {
        return flow {
            try {
                val cachedCountry = countryDao.getCountryByCode(countryCode)
                if (cachedCountry != null) {
                    emit(cachedCountry.toCountryModel())
                    return@flow
                }

                getAllCountries().collect { countries ->
                    val country = countries.find { it.code == countryCode }
                    emit(country)
                }
            } catch (e: Exception) {
                Log.e("CountryRepository", "Error getting country by code: ${e.message}", e)
                emit(null)
            }
        }
    }

    override suspend fun searchCountries(query: String, limit: Int): Flow<List<CountryModel>> {
        return flow {
            try {
                getAllCountries().collect { countries ->
                    val filtered = countries.filter { country ->
                        country.code.contains(query, ignoreCase = true) ||
                        country.nameKey.contains(query, ignoreCase = true)
                    }.take(limit)
                    emit(filtered)
                }
            } catch (e: Exception) {
                Log.e("CountryRepository", "Error searching countries: ${e.message}", e)
                emit(emptyList())
            }
        }
    }

    override suspend fun getCachedCountries(): Flow<List<CountryModel>> {
        return flow {
            try {
                val cachedCountries = countryDao.getAllCountries()
                emit(cachedCountries.map { it.toCountryModel() })
            } catch (e: Exception) {
                Log.e("CountryRepository", "Error getting cached countries: ${e.message}", e)
                emit(emptyList())
            }
        }
    }

    override suspend fun getCachedCountryCollections(userId: Long): Flow<List<CountryCollectionModel>> {
        return flow {
            try {
                val cachedCollections = countryDao.getCountryCollectionsByUser(userId)
                emit(cachedCollections.map { it.toCountryCollectionModel() })
            } catch (e: Exception) {
                Log.e("CountryRepository", "Error getting cached collections: ${e.message}", e)
                emit(emptyList())
            }
        }
    }

    override suspend fun refreshCountryCollections(): Result<Unit> {
        return try {
            val response = apiService.getCurrentUserCountryCollections()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshCountriesData(): Result<Unit> {
        return try {
            val response = apiService.getAllCountries()
            if (response.isSuccessful) {
                response.body()?.let { countries ->
                    val countryEntities = countries.map { it.toCountryEntity() }
                    countryDao.insertCountries(countryEntities)
                    Result.success(Unit)
                } ?: Result.failure(Exception(context.getString(R.string.error_empty_response)))
            } else {
                val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCountriesVisitedCount(userId: Long): Flow<Int> {
        return flow {
            try {
                getUserCountryCollections(userId).collect { collections ->
                    emit(collections.totalCountriesVisited)
                }
            } catch (e: Exception) {
                Log.e("CountryRepository", "Error getting countries count: ${e.message}", e)
                emit(0)
            }
        }
    }

    override suspend fun getTotalCitiesVisited(userId: Long): Flow<Int> {
        return flow {
            try {
                getUserCountryCollections(userId).collect { collections ->
                    emit(collections.totalCitiesVisited)
                }
            } catch (e: Exception) {
                Log.e("CountryRepository", "Error getting cities count: ${e.message}", e)
                emit(0)
            }
        }
    }

    override suspend fun getTotalPostsCount(userId: Long): Flow<Int> {
        return flow {
            try {
                getUserCountryCollections(userId).collect { collections ->
                    emit(collections.totalPostsCount)
                }
            } catch (e: Exception) {
                Log.e("CountryRepository", "Error getting posts count: ${e.message}", e)
                emit(0)
            }
        }
    }
}

private fun Country.toCountryModel(): CountryModel {
    return CountryModel(
        id = id,
        code = code,
        nameKey = nameKey,
        latitude = latitude,
        longitude = longitude
    )
}

private fun CountryModel.toCountryEntity(): Country {
    return Country(
        id = id,
        code = code,
        nameKey = nameKey,
        latitude = latitude,
        longitude = longitude,
        cacheTimestamp = System.currentTimeMillis()
    )
}

private fun CountryCollection.toCountryCollectionModel(): CountryCollectionModel {
    return CountryCollectionModel(
        countryId = countryId,
        countryCode = countryCode,
        countryNameKey = countryNameKey,
        firstVisitDate = firstVisitDate,
        lastVisitDate = lastVisitDate,
        visitCount = visitCount,
        postsCount = postsCount
    )
}

private fun isCacheValid(timestamp: Long): Boolean {
    val cacheExpiryMs = 12 * 60 * 60 * 1000L
    return (System.currentTimeMillis() - timestamp) < cacheExpiryMs
}