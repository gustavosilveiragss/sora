package com.sora.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sora.android.data.local.entity.ProfileCountryMarkerEntity
import com.sora.android.data.local.entity.ProfileGlobeDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GlobeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfileGlobeData(profileGlobeData: ProfileGlobeDataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfileCountryMarkers(profileCountryMarkers: List<ProfileCountryMarkerEntity>)

    @Query("SELECT * FROM profile_globe_data WHERE userId = :userId")
    fun getProfileGlobeData(userId: Long): Flow<ProfileGlobeDataEntity?>

    @Query("SELECT * FROM profile_country_marker WHERE userId = :userId")
    fun getProfileCountryMarkers(userId: Long): Flow<List<ProfileCountryMarkerEntity>>

    @Query("DELETE FROM profile_globe_data WHERE userId = :userId")
    suspend fun deleteProfileGlobeData(userId: Long)
}