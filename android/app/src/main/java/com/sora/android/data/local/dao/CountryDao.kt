package com.sora.android.data.local.dao

import androidx.room.*
import com.sora.android.data.local.entity.Country
import com.sora.android.data.local.entity.CountryCollection
import kotlinx.coroutines.flow.Flow

@Dao
interface CountryDao {

    @Query("SELECT * FROM country ORDER BY nameKey ASC")
    suspend fun getAllCountries(): List<Country>

    @Query("SELECT * FROM country WHERE id = :countryId")
    suspend fun getCountryById(countryId: Long): Country?

    @Query("SELECT * FROM country WHERE code = :countryCode")
    suspend fun getCountryByCode(countryCode: String): Country?

    @Query("SELECT * FROM country WHERE nameKey LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%' ORDER BY nameKey ASC LIMIT :limit")
    suspend fun searchCountries(query: String, limit: Int = 10): List<Country>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountry(country: Country)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountries(countries: List<Country>)

    @Update
    suspend fun updateCountry(country: Country)

    @Delete
    suspend fun deleteCountry(country: Country)

    @Query("DELETE FROM country WHERE cacheTimestamp < :expiry")
    suspend fun deleteExpiredCountries(expiry: Long): Int

    @Query("SELECT * FROM country_collection WHERE userId = :userId ORDER BY visitCount DESC, lastVisitDate DESC")
    suspend fun getCountryCollectionsByUser(userId: Long): List<CountryCollection>

    @Query("SELECT * FROM country_collection WHERE userId = :userId ORDER BY visitCount DESC, lastVisitDate DESC")
    fun getCountryCollectionsByUserFlow(userId: Long): Flow<List<CountryCollection>>

    @Query("SELECT * FROM country_collection WHERE userId = :userId AND countryCode = :countryCode")
    suspend fun getCountryCollection(userId: Long, countryCode: String): CountryCollection?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountryCollection(collection: CountryCollection)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountryCollections(collections: List<CountryCollection>)

    @Update
    suspend fun updateCountryCollection(collection: CountryCollection)

    @Delete
    suspend fun deleteCountryCollection(collection: CountryCollection)

    @Query("DELETE FROM country_collection WHERE cacheTimestamp < :expiry")
    suspend fun deleteExpiredCountryCollections(expiry: Long): Int

    @Query("SELECT COUNT(*) FROM country_collection WHERE userId = :userId")
    suspend fun getCountriesVisitedCount(userId: Long): Int
}