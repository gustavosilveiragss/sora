package com.sora.android.data.local.dao

import androidx.room.*
import com.sora.android.data.local.entity.City

@Dao
interface CityDao {

    @Query("SELECT * FROM city ORDER BY name ASC")
    suspend fun getAllCities(): List<City>

    @Query("SELECT * FROM city WHERE id = :cityId")
    suspend fun getCityById(cityId: Long): City?

    @Query("SELECT * FROM city WHERE countryId = :countryId ORDER BY name ASC")
    suspend fun getCitiesByCountry(countryId: Long): List<City>

    @Query("SELECT * FROM city WHERE countryCode = :countryCode ORDER BY name ASC")
    suspend fun getCitiesByCountryCode(countryCode: String): List<City>

    @Query("SELECT * FROM city WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchCities(query: String): List<City>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCity(city: City)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCities(cities: List<City>)

    @Update
    suspend fun updateCity(city: City)

    @Delete
    suspend fun deleteCity(city: City)

    @Query("DELETE FROM city WHERE cacheTimestamp < :expiry")
    suspend fun deleteExpiredCities(expiry: Long): Int

    @Query("SELECT COUNT(*) FROM city")
    suspend fun getCityCount(): Int
}