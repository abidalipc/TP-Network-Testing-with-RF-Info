package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.*
import com.example.data.kmz.KmzParser
import com.example.data.kmz.SampleKmzData
import com.example.location.DistanceCalculator
import com.example.location.LocationData
import com.example.location.LocationHelper
import com.example.telecom.CellInfoData
import com.example.notification.NotificationHelper
import com.example.telecom.CellMonitor
import com.example.tts.VoiceNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class UiState(
    val location: LocationData = LocationData(),
    val cellInfo: CellInfoData = CellInfoData(),
    val sites: List<SiteEntity> = emptyList(),
    val sectorsMap: Map<String, List<SectorEntity>> = emptyMap(),
    val matchedSite: SiteEntity? = null,
    val matchedSector: SectorEntity? = null,
    val distanceToSiteMeters: Double = 0.0,
    val bearingToSiteDegrees: Float = 0f,
    val isSimulationMode: Boolean = false,
    val isVoiceEnabled: Boolean = true,
    val isAutoRefresh: Boolean = true,
    val lastRefreshTimeMs: Long = System.currentTimeMillis(),
    val refreshCount: Int = 0,
    val lastRefreshNotification: String = "",
    val logs: List<LogEntity> = emptyList(),
    val statusMessage: String = "Ready"
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.telecomDao()

    private val locationHelper = LocationHelper(application)
    val cellMonitor = CellMonitor(application)
    val voiceManager = VoiceNotificationManager(application)
    val notificationHelper = NotificationHelper(application)
    private val kmzParser = KmzParser(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        // Load initial data
        observeDatabase()
        startLocationTracking()
        startCellMonitoring()
        loadSampleKmzIfEmpty()
    }

    private fun observeDatabase() {
        viewModelScope.launch {
            dao.getAllSites().combine(dao.getAllSectors()) { sites, sectors ->
                val secMap = sectors.groupBy { it.siteId }
                Pair(sites, secMap)
            }.collect { (sites, secMap) ->
                _uiState.update { state ->
                    val matchedSite = sites.firstOrNull { it.siteId == state.cellInfo.siteId }
                        ?: findClosestSite(state.location, sites)
                    val matchedSectors = secMap[matchedSite?.siteId] ?: emptyList()
                    val matchedSector = matchedSectors.firstOrNull { it.sectorId == state.cellInfo.sectorId }
                        ?: matchedSectors.firstOrNull()

                    val (dist, bearing) = calculateDistanceAndBearing(state.location, matchedSite)

                    state.copy(
                        sites = sites,
                        sectorsMap = secMap,
                        matchedSite = matchedSite,
                        matchedSector = matchedSector,
                        distanceToSiteMeters = dist,
                        bearingToSiteDegrees = bearing
                    )
                }
            }
        }

        viewModelScope.launch {
            dao.getRecentLogs().collect { logs ->
                _uiState.update { it.copy(logs = logs) }
            }
        }
    }

    private fun startLocationTracking() {
        viewModelScope.launch {
            locationHelper.getLocationUpdates(3000L).collect { location ->
                _uiState.update { state ->
                    val (dist, bearing) = calculateDistanceAndBearing(location, state.matchedSite)
                    state.copy(
                        location = location,
                        distanceToSiteMeters = dist,
                        bearingToSiteDegrees = bearing
                    )
                }
            }
        }
    }

    private fun startCellMonitoring() {
        refreshNetworkData()
        startAutoRefreshLoop()
    }

    fun setAutoRefreshMode(enabled: Boolean) {
        _uiState.update { it.copy(isAutoRefresh = enabled) }
        if (enabled) {
            startAutoRefreshLoop()
        } else {
            autoRefreshJob?.cancel()
            autoRefreshJob = null
        }
    }

    private fun startAutoRefreshLoop() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive && _uiState.value.isAutoRefresh) {
                delay(15000L) // 15 seconds interval
                if (isActive && _uiState.value.isAutoRefresh) {
                    refreshNetworkData()
                }
            }
        }
    }

    fun refreshNetworkData() {
        viewModelScope.launch {
            val cellInfo = cellMonitor.readCellInfoData()
            val currentState = _uiState.value
            val sites = currentState.sites
            val matchedSite = sites.firstOrNull { it.siteId == cellInfo.siteId }
                ?: findClosestSite(currentState.location, sites)
            val matchedSectors = currentState.sectorsMap[matchedSite?.siteId] ?: emptyList()
            val matchedSector = matchedSectors.firstOrNull { it.sectorId == cellInfo.sectorId }
                ?: matchedSectors.firstOrNull()

            val (dist, bearing) = calculateDistanceAndBearing(currentState.location, matchedSite)

            val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            val timeStr = timeFormat.format(java.util.Date())
            val notifyMsg = "Site ${cellInfo.siteId}, Sector ${cellInfo.sectorId} • ${cellInfo.frequencyLabel} (RSRP: ${cellInfo.rsrp} dBm)"

            // Send Notification (System Notification + Toast) with every refresh
            notificationHelper.showRefreshNotification(
                title = "Network Refreshed ($timeStr)",
                message = notifyMsg
            )

            // Voice Alert Trigger with every refresh
            if (currentState.isVoiceEnabled) {
                voiceManager.announceCurrentTelemetry(
                    siteId = cellInfo.siteId,
                    sectorId = cellInfo.sectorId,
                    bandName = cellInfo.frequencyLabel,
                    siteName = matchedSite?.siteName,
                    distanceMeters = if (dist > 0) dist else null
                )
            }

            // Insert into log
            if (cellInfo.siteId != "N/A" && currentState.location.isValid) {
                dao.insertLog(
                    LogEntity(
                        latitude = currentState.location.latitude,
                        longitude = currentState.location.longitude,
                        siteId = cellInfo.siteId,
                        sectorId = cellInfo.sectorId,
                        band = cellInfo.frequencyLabel,
                        rsrp = cellInfo.rsrp,
                        rsrq = cellInfo.rsrq,
                        distanceMeters = dist
                    )
                )
            }

            _uiState.update { state ->
                val newCount = state.refreshCount + 1
                state.copy(
                    cellInfo = cellInfo,
                    matchedSite = matchedSite,
                    matchedSector = matchedSector,
                    distanceToSiteMeters = dist,
                    bearingToSiteDegrees = bearing,
                    lastRefreshTimeMs = System.currentTimeMillis(),
                    refreshCount = newCount,
                    lastRefreshNotification = "Refreshed #$newCount at $timeStr: $notifyMsg",
                    statusMessage = "Network Refreshed #$newCount at $timeStr"
                )
            }
        }
    }

    private fun calculateDistanceAndBearing(
        location: LocationData,
        site: SiteEntity?
    ): Pair<Double, Float> {
        if (!location.isValid || site == null) return Pair(0.0, 0f)
        val dist = DistanceCalculator.calculateDistanceMeters(
            location.latitude, location.longitude,
            site.latitude, site.longitude
        )
        val bearing = DistanceCalculator.calculateBearing(
            location.latitude, location.longitude,
            site.latitude, site.longitude
        )
        return Pair(dist, bearing)
    }

    private fun findClosestSite(location: LocationData, sites: List<SiteEntity>): SiteEntity? {
        if (!location.isValid || sites.isEmpty()) return sites.firstOrNull()
        return sites.minByOrNull { site ->
            DistanceCalculator.calculateDistanceMeters(
                location.latitude, location.longitude,
                site.latitude, site.longitude
            )
        }
    }

    private fun loadSampleKmzIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            val (sites, sectors) = SampleKmzData.getSampleSitesAndSectors()
            dao.insertSites(sites)
            dao.insertSectors(sectors)
        }
    }

    fun importKmzFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(statusMessage = "Parsing KMZ File...") }
            val parsed = kmzParser.parseKmzOrKmlUri(uri)
            if (parsed.sites.isNotEmpty()) {
                dao.insertSites(parsed.sites)
                dao.insertSectors(parsed.sectors)
                _uiState.update {
                    it.copy(statusMessage = "Successfully imported ${parsed.sites.size} sites and ${parsed.sectors.size} sectors!")
                }
            } else {
                _uiState.update { it.copy(statusMessage = "No valid site placemarks found in file.") }
            }
        }
    }

    fun loadSampleKmzData() {
        viewModelScope.launch(Dispatchers.IO) {
            val parsed = kmzParser.parseKmlString(SampleKmzData.getSampleKmlXml())
            dao.insertSites(parsed.sites)
            dao.insertSectors(parsed.sectors)
            _uiState.update {
                it.copy(statusMessage = "Loaded sample network with ${parsed.sites.size} sites across 800, 900, 1800, 2100 & 2600 MHz!")
            }
        }
    }

    fun clearAllSites() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAllSites()
            dao.deleteAllSectors()
            _uiState.update { it.copy(statusMessage = "Cleared site database.") }
        }
    }

    fun toggleSimulationMode(enabled: Boolean) {
        cellMonitor.isSimulationMode = enabled
        _uiState.update { it.copy(isSimulationMode = enabled) }
    }

    fun setSimulationBand(earfcn: Int, siteId: String = "104", sectorId: String = "2") {
        cellMonitor.setSimulationParameters(siteId, sectorId, earfcn)
        refreshNetworkData()
    }

    fun toggleVoiceAlerts(enabled: Boolean) {
        voiceManager.isVoiceEnabled = enabled
        if (!enabled) {
            voiceManager.stop()
        }
        _uiState.update { it.copy(isVoiceEnabled = enabled) }
    }

    fun announceCurrentTelemetry() {
        val state = _uiState.value
        if (state.isVoiceEnabled) {
            voiceManager.announceCurrentTelemetry(
                siteId = state.cellInfo.siteId,
                sectorId = state.cellInfo.sectorId,
                bandName = state.cellInfo.frequencyLabel,
                siteName = state.matchedSite?.siteName,
                distanceMeters = if (state.distanceToSiteMeters > 0) state.distanceToSiteMeters else null
            )
        }
    }

    fun speakTestAlert() {
        if (_uiState.value.isVoiceEnabled) {
            voiceManager.speak("Site and Sector tracer voice notifications are active.")
        }
    }

    fun speakCustomText(text: String) {
        if (_uiState.value.isVoiceEnabled) {
            voiceManager.speak(text)
        }
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearLogs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutdown()
    }
}
