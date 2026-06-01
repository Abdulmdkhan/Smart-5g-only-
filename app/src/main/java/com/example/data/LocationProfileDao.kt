package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationProfileDao {
    @Query("SELECT * FROM location_profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<LocationProfile>>

    @Query("SELECT * FROM location_profiles WHERE isEnabled = 1")
    fun getActiveProfiles(): Flow<List<LocationProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: LocationProfile)

    @Update
    suspend fun updateProfile(profile: LocationProfile)

    @Delete
    suspend fun deleteProfile(profile: LocationProfile)

    @Query("DELETE FROM location_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Int)
}
