package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_profiles")
data class LocationProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusInMeters: Float = 200f,
    val preferredMode: String, // "5G Only", "4G Only", "Auto"
    val isEnabled: Boolean = true
)
