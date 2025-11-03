package com.sora.android.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.sora.android.R
import com.sora.android.data.remote.ApiService
import com.sora.android.data.remote.util.NetworkUtils
import com.sora.android.domain.model.*
import com.sora.android.domain.repository.LocationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) : LocationRepository {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    override suspend fun searchLocations(
        query: String,
        countryCode: String?,
        limit: Int
    ): Flow<LocationSearchResultModel> {
        return flow {
            try {
                Log.d("SORA_LOCATION", "Buscando localizacoes: query=$query, countryCode=$countryCode")

                val response = apiService.searchLocations(query, countryCode, limit)
                if (response.isSuccessful) {
                    response.body()?.let { result ->
                        Log.d("SORA_LOCATION", "Localizacoes encontradas: ${result.results.size}")
                        emit(result)
                    }
                } else {
                    val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                    Log.e("SORA_LOCATION", "Erro ao buscar localizacoes: $errorMessage")
                    throw Exception(errorMessage)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SORA_LOCATION", "Excecao ao buscar localizacoes: ${e.message}", e)
                throw e
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): Flow<ReverseGeocodeResultModel> {
        return flow {
            try {
                Log.d("SORA_LOCATION", "Reverse geocoding: lat=$latitude, lon=$longitude")

                val response = apiService.reverseGeocode(latitude, longitude)
                if (response.isSuccessful) {
                    response.body()?.let { result ->
                        Log.d("SORA_LOCATION", "Reverse geocode bem-sucedido: ${result.displayName}")
                        emit(result)
                    }
                } else {
                    val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                    Log.e("SORA_LOCATION", "Erro no reverse geocode: $errorMessage")
                    throw Exception(errorMessage)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SORA_LOCATION", "Excecao no reverse geocode: ${e.message}", e)
                throw e
            }
        }
    }

    override suspend fun searchCitiesInCountry(
        countryCode: String,
        query: String,
        limit: Int
    ): Flow<List<SearchLocationModel>> {
        return flow {
            try {
                Log.d("SORA_LOCATION", "Buscando cidades: countryCode=$countryCode, query=$query")

                val response = apiService.searchCitiesInCountry(countryCode, query, limit)
                if (response.isSuccessful) {
                    response.body()?.let { searchResult ->
                        Log.d("SORA_LOCATION", "Cidades encontradas: ${searchResult.results.size}")
                        emit(searchResult.results)
                    } ?: emit(emptyList())
                } else {
                    val errorMessage = NetworkUtils.parseErrorMessage(response, context)
                    Log.e("SORA_LOCATION", "Erro ao buscar cidades: $errorMessage")
                    emit(emptyList())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SORA_LOCATION", "Excecao ao buscar cidades: ${e.message}", e)
                emit(emptyList())
            }
        }
    }

    override suspend fun getPopularDestinations(
        limit: Int,
        days: Int
    ): Flow<List<CountryModel>> {
        return flow {
            try {
                Log.d("SORA_LOCATION", "Buscando destinos populares: limit=$limit, days=$days")

                val response = apiService.getPopularDestinations(limit, days)
                if (response.isSuccessful) {
                    response.body()?.let { countries ->
                        Log.d("SORA_LOCATION", "Destinos populares: ${countries.size}")
                        emit(countries)
                    } ?: emit(emptyList())
                } else {
                    emit(emptyList())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SORA_LOCATION", "Excecao ao buscar destinos populares: ${e.message}", e)
                emit(emptyList())
            }
        }
    }

    override suspend fun getCachedLocations(query: String): Flow<List<SearchLocationModel>> {
        return flow {
            emit(emptyList())
        }
    }

    override suspend fun getCachedCities(countryCode: String): Flow<List<CityModel>> {
        return flow {
            emit(emptyList())
        }
    }

    override suspend fun refreshPopularDestinations(): Result<Unit> {
        return try {
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun validateCoordinates(latitude: Double, longitude: Double): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    override suspend fun getCountryFromCoordinates(
        latitude: Double,
        longitude: Double
    ): Flow<CountryModel?> {
        return flow {
            try {
                reverseGeocode(latitude, longitude).collect { result ->
                    emit(null)
                }
            } catch (e: Exception) {
                emit(null)
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return try {
            Log.d("SORA_LOCATION", "Obtendo localizacao atual")
            val cancellationToken = CancellationTokenSource()

            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationToken.token
            ).await()

            Log.d("SORA_LOCATION", "Localizacao obtida: lat=${location?.latitude}, lon=${location?.longitude}")
            location
        } catch (e: Exception) {
            Log.e("SORA_LOCATION", "Erro ao obter localizacao: ${e.message}", e)
            null
        }
    }
}
