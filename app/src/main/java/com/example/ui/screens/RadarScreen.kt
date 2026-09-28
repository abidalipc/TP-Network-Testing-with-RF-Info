package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.SiteEntity
import com.example.location.DistanceCalculator
import com.example.ui.UiState
import com.example.ui.components.CellRadarView
import com.example.ui.components.GoogleSiteMapView

enum class RadarViewMode {
    GOOGLE_MAPS,
    TACTICAL_RADAR
}

@Composable
fun RadarScreen(
    uiState: UiState,
    onSpeakTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSiteState by remember { mutableStateOf<SiteEntity?>(null) }
    var viewMode by remember { mutableStateOf(RadarViewMode.GOOGLE_MAPS) }
    val siteToShow = selectedSiteState ?: uiState.matchedSite

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // View Selector Segmented Control
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = viewMode == RadarViewMode.GOOGLE_MAPS,
                onClick = { viewMode = RadarViewMode.GOOGLE_MAPS },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = { Icon(Icons.Default.Map, contentDescription = "Google Maps", modifier = Modifier.size(16.dp)) }
            ) {
                Text("Google Map", fontWeight = FontWeight.SemiBold)
            }
            SegmentedButton(
                selected = viewMode == RadarViewMode.TACTICAL_RADAR,
                onClick = { viewMode = RadarViewMode.TACTICAL_RADAR },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = { Icon(Icons.Default.Radar, contentDescription = "Tactical Radar", modifier = Modifier.size(16.dp)) }
            ) {
                Text("Radar Scope", fontWeight = FontWeight.SemiBold)
            }
        }

        // Selected Map View
        when (viewMode) {
            RadarViewMode.GOOGLE_MAPS -> {
                GoogleSiteMapView(
                    userLocation = uiState.location,
                    sites = uiState.sites,
                    sectorsMap = uiState.sectorsMap,
                    activeSiteId = uiState.cellInfo.siteId,
                    activeSectorId = uiState.cellInfo.sectorId,
                    onSiteSelected = { site ->
                        selectedSiteState = site
                    }
                )
            }
            RadarViewMode.TACTICAL_RADAR -> {
                CellRadarView(
                    userLocation = uiState.location,
                    sites = uiState.sites,
                    sectorsMap = uiState.sectorsMap,
                    activeSiteId = uiState.cellInfo.siteId,
                    activeSectorId = uiState.cellInfo.sectorId,
                    onSiteSelected = { site ->
                        selectedSiteState = site
                    }
                )
            }
        }

        // Selected Site Detail Panel
        siteToShow?.let { site ->
            val sectors = uiState.sectorsMap[site.siteId] ?: emptyList()
            val distMeters = DistanceCalculator.calculateDistanceMeters(
                uiState.location.latitude, uiState.location.longitude,
                site.latitude, site.longitude
            )
            val bearing = DistanceCalculator.calculateBearing(
                uiState.location.latitude, uiState.location.longitude,
                site.latitude, site.longitude
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SITE ${site.siteId}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = site.siteName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = onSpeakTest,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Voice Announce",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Announce", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Distance: ${DistanceCalculator.formatDistance(distMeters)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Bearing: ${bearing.toInt()}°",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "Sectors (${sectors.size}):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                    ) {
                        items(sectors) { sector ->
                            val isActive = site.siteId == uiState.cellInfo.siteId && sector.sectorId == uiState.cellInfo.sectorId
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Sector ${sector.sectorId} (${sector.sectorName})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Azimuth: ${sector.azimuth.toInt()}° • Beam: ${sector.beamwidth.toInt()}°",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        Text(
                                            text = "${sector.band} MHz",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
