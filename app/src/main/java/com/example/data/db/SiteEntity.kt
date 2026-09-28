package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey val siteId: String,
    val siteName: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val technology: String = "LTE",
    val address: String = "",
    val importedAt: Long = System.currentTimeMillis()
)
