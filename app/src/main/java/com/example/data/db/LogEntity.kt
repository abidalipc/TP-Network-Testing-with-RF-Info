package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telecom_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double,
    val siteId: String,
    val sectorId: String,
    val band: String,
    val rsrp: Int,
    val rsrq: Int,
    val distanceMeters: Double
)
