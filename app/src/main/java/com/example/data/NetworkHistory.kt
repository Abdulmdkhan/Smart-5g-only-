package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_history")
data class NetworkHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val modeFrom: String,
    val modeTo: String,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean = true,
    val simSlot: Int = -1,
    val speedMbps: Float = 0f,
    val note: String = ""
)
