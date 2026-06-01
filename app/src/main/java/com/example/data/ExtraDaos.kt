package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkHistoryDao {
    @Query("SELECT * FROM network_history ORDER BY timestamp DESC LIMIT 100")
    fun getHistoryFlow(): Flow<List<NetworkHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: NetworkHistory)

    @Query("DELETE FROM network_history")
    suspend fun clearHistory()
}

@Dao
interface NetworkScheduleDao {
    @Query("SELECT * FROM network_schedules ORDER BY id DESC")
    fun getSchedulesFlow(): Flow<List<NetworkSchedule>>

    @Query("SELECT * FROM network_schedules WHERE isEnabled = 1")
    suspend fun getActiveSchedules(): List<NetworkSchedule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: NetworkSchedule)

    @Update
    suspend fun updateSchedule(schedule: NetworkSchedule)

    @Delete
    suspend fun deleteSchedule(schedule: NetworkSchedule)

    @Query("DELETE FROM network_schedules WHERE id = :id")
    suspend fun deleteScheduleById(id: Int)
}

@Dao
interface FavoriteModeDao {
    @Query("SELECT * FROM favorite_modes ORDER BY position ASC")
    fun getFavoriteModesFlow(): Flow<List<FavoriteMode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteMode)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteMode)

    @Query("DELETE FROM favorite_modes WHERE modeName = :modeName")
    suspend fun deleteFavoriteByName(modeName: String)
}
