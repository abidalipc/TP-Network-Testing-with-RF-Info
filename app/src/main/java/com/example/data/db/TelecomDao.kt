package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TelecomDao {

    @Query("SELECT * FROM sites ORDER BY siteId ASC")
    fun getAllSites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE siteId = :siteId LIMIT 1")
    suspend fun getSiteById(siteId: String): SiteEntity?

    @Query("SELECT * FROM sectors WHERE siteId = :siteId")
    fun getSectorsForSite(siteId: String): Flow<List<SectorEntity>>

    @Query("SELECT * FROM sectors")
    fun getAllSectors(): Flow<List<SectorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSites(sites: List<SiteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSectors(sectors: List<SectorEntity>)

    @Query("DELETE FROM sites")
    suspend fun deleteAllSites()

    @Query("DELETE FROM sectors")
    suspend fun deleteAllSectors()

    @Query("SELECT * FROM telecom_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentLogs(): Flow<List<LogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity)

    @Query("DELETE FROM telecom_logs")
    suspend fun clearLogs()
}
