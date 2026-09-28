package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.SiteEntity
import com.example.location.DistanceCalculator
import com.example.ui.UiState
import com.example.ui.components.GoogleSiteMapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    uiState: UiState,
    onSpeakTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSiteState by remember { mutableStateOf<SiteEntity?>(null) }
    var showSitesListSheet by remember { mutableStateOf(false) }
    val siteToShow = selectedSiteState ?: uiState.matchedSite

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A1526))
    ) {
        // Edge-to-Edge Map Canvas Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            GoogleSiteMapView(
                userLocation = uiState.location,
                sites = uiState.sites,
                sectorsMap = uiState.sectorsMap,
                activeSiteId = uiState.cellInfo.siteId,
                activeSectorId = uiState.cellInfo.sectorId,
                onSiteSelected = { site ->
                    selectedSiteState = site
                },
                modifier = Modifier.fillMaxSize()
            )

            // Selected Site Detail Overlay Card at bottom
            siteToShow?.let { site ->
                val sectors = uiState.sectorsMap[site.siteId] ?: emptyList()
                val distMeters = DistanceCalculator.calculateDistanceMeters(
                    uiState.location.latitude,
                    uiState.location.longitude,
                    site.latitude,
                    site.longitude
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CellTower,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = site.siteName.ifEmpty { "Site ${site.siteId}" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${site.address.ifEmpty { "Tower ID: ${site.siteId}" }} • ${sectors.size} Sectors",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "${distMeters.toInt()}m away",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Sites List Bottom Sheet
    if (showSitesListSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSitesListSheet = false },
            containerColor = Color(0xFF0F172A),
            scrimColor = Color.Black.copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Nearby Cell Towers (${uiState.sites.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Divider(color = Color(0xFF1E293B))

                uiState.sites.forEach { site ->
                    val isCurrentActive = site.siteId == uiState.cellInfo.siteId || site.siteName == "Khwazakhela"
                    Card(
                        onClick = {
                            selectedSiteState = site
                            showSitesListSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentActive) Color(0xFF0284C7).copy(alpha = 0.3f) else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CellTower,
                                    contentDescription = null,
                                    tint = if (isCurrentActive) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = site.siteName.ifEmpty { "Site ${site.siteId}" },
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "ID: ${site.siteId} • Lat: ${String.format("%.4f", site.latitude)}, Lng: ${String.format("%.4f", site.longitude)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            if (isCurrentActive) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF0284C7)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

