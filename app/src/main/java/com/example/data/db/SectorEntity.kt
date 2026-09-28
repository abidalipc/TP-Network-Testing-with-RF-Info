package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sectors")
data class SectorEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val siteId: String,
    val sectorId: String,
    val sectorName: String = "",
    val azimuth: Float = 0f,      // Beam direction in degrees (0..360)
    val beamwidth: Float = 65f,   // Sector beam spread in degrees
    val band: String = "1800",    // Band label e.g. 800, 900, 1800, 2100, 2600
    val earfcn: Int = 0,
    val pci: Int = 0
)
