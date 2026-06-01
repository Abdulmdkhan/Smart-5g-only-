package com.example.data

import kotlinx.coroutines.flow.Flow

class LocationProfileRepository(private val dao: LocationProfileDao) {
    val allProfiles: Flow<List<LocationProfile>> = dao.getAllProfiles()
    val activeProfiles: Flow<List<LocationProfile>> = dao.getActiveProfiles()

    suspend fun insert(profile: LocationProfile) {
        dao.insertProfile(profile)
    }

    suspend fun update(profile: LocationProfile) {
        dao.updateProfile(profile)
    }

    suspend fun delete(profile: LocationProfile) {
        dao.deleteProfile(profile)
    }

    suspend fun deleteById(id: Int) {
        dao.deleteProfileById(id)
    }
}
