package com.example.data

import kotlinx.coroutines.flow.Flow

class ExtraRepository(
    private val historyDao: NetworkHistoryDao,
    private val scheduleDao: NetworkScheduleDao,
    private val favoriteModeDao: FavoriteModeDao
) {
    // History Flows
    val networkHistory: Flow<List<NetworkHistory>> = historyDao.getHistoryFlow()

    suspend fun addHistoryEntry(entry: NetworkHistory) {
        historyDao.insertHistory(entry)
    }

    suspend fun clearHistoryLog() {
        historyDao.clearHistory()
    }

    // Schedule Flows
    val allSchedules: Flow<List<NetworkSchedule>> = scheduleDao.getSchedulesFlow()

    suspend fun getActiveSchedulesList(): List<NetworkSchedule> {
        return scheduleDao.getActiveSchedules()
    }

    suspend fun insertSchedule(schedule: NetworkSchedule) {
        scheduleDao.insertSchedule(schedule)
    }

    suspend fun updateSchedule(schedule: NetworkSchedule) {
        scheduleDao.updateSchedule(schedule)
    }

    suspend fun deleteSchedule(schedule: NetworkSchedule) {
        scheduleDao.deleteSchedule(schedule)
    }

    suspend fun deleteScheduleById(id: Int) {
        scheduleDao.deleteScheduleById(id)
    }

    // Favorite Modes Flow
    val favoriteModes: Flow<List<FavoriteMode>> = favoriteModeDao.getFavoriteModesFlow()

    suspend fun insertFavorite(favorite: FavoriteMode) {
        favoriteModeDao.insertFavorite(favorite)
    }

    suspend fun removeFavorite(favorite: FavoriteMode) {
        favoriteModeDao.deleteFavorite(favorite)
    }

    suspend fun removeFavoriteByName(name: String) {
        favoriteModeDao.deleteFavoriteByName(name)
    }
}
