package com.sora.android.core.startup

import android.util.Log
import com.sora.android.domain.repository.CountryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataInitializer @Inject constructor(
    private val countryRepository: CountryRepository
) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initializeEssentialData() {
        Log.d("DataInitializer", "Starting essential data initialization...")

        applicationScope.launch {
            try {
                Log.d("DataInitializer", "Loading countries data...")

                countryRepository.getAllCountries().collect { countries ->
                    Log.d("DataInitializer", "Loaded ${countries.size} countries into cache")

                    if (countries.isNotEmpty()) {
                        Log.d("DataInitializer", "Sample countries: ${countries.take(3).map { it.code }}")
                    } else {
                        Log.w("DataInitializer", "No countries loaded - this will cause issues with user data!")
                    }
                }

                Log.d("DataInitializer", "Essential data initialization completed")
            } catch (e: Exception) {
                Log.e("DataInitializer", "Error during data initialization: ${e.message}", e)
            }
        }
    }

    fun preloadUserData(userId: Long) {
        Log.d("DataInitializer", "Preloading data for user: $userId")

        applicationScope.launch {
            try {
                countryRepository.getUserCountryCollections(userId).collect { collections ->
                    Log.d("DataInitializer", "User has visited ${collections.totalCountriesVisited} countries")
                }
            } catch (e: Exception) {
                Log.e("DataInitializer", "Error preloading user data: ${e.message}", e)
            }
        }
    }
}