package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_schedules")
data class NetworkSchedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val targetMode: String,
    val activeDays: String, // "weekdays", "weekend", "everyday"
    val isEnabled: Boolean = true
)
