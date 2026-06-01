package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_modes")
data class FavoriteMode(
    @PrimaryKey val modeName: String,
    val position: Int = 0
)
