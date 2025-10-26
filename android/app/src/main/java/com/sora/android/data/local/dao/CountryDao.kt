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

    @Query("SELECT COUNT(*) FROM country_collection WHERE userId = :userId")
    fun getCountriesVisitedCountFlow(userId: Long): Flow<Int>

    @Query("UPDATE country_collection SET postsCount = postsCount + 1, lastVisitDate = :visitDate WHERE userId = :userId AND countryCode = :countryCode")
    suspend fun incrementCountryPostsCount(userId: Long, countryCode: String, visitDate: String)

    @Query("UPDATE country_collection SET postsCount = postsCount - 1 WHERE userId = :userId AND countryCode = :countryCode AND postsCount > 0")
    suspend fun decrementCountryPostsCount(userId: Long, countryCode: String)

    @Query("UPDATE country_collection SET visitCount = visitCount + 1, lastVisitDate = :visitDate WHERE userId = :userId AND countryCode = :countryCode")
    suspend fun incrementCountryVisitCount(userId: Long, countryCode: String, visitDate: String)

    @Query("UPDATE country SET cacheTimestamp = :timestamp WHERE id = :countryId")
    suspend fun touchCountryCache(countryId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE country_collection SET cacheTimestamp = :timestamp WHERE id = :collectionId")
    suspend fun touchCountryCollectionCache(collectionId: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM country")
    suspend fun getCountryCount(): Int

    @Transaction
    suspend fun createOrUpdateCountryCollection(
        userId: Long,
        countryId: Long,
        countryCode: String,
        countryNameKey: String,
        visitDate: String
    ) {
        val existing = getCountryCollection(userId, countryCode)
        if (existing != null) {
            incrementCountryPostsCount(userId, countryCode, visitDate)
        } else {
            val newCollection = CountryCollection(
                id = "${userId}_${countryCode}",
                userId = userId,
                countryId = countryId,
                countryCode = countryCode,
                countryNameKey = countryNameKey,
                firstVisitDate = visitDate,
                lastVisitDate = visitDate,
                visitCount = 1,
                postsCount = 1
            )
            insertCountryCollection(newCollection)
        }
    }
}