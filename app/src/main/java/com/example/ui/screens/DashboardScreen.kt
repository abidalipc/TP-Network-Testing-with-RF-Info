package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.location.DistanceCalculator
import com.example.ui.UiState
import com.example.ui.components.BandChip
import com.example.ui.components.DistanceCompassCard
import com.example.ui.components.SignalGaugeCard

@Composable
fun DashboardScreen(
    uiState: UiState,
    onToggleVoiceAlerts: (Boolean) -> Unit,
    onTestVoiceAlert: () -> Unit,
    onToggleAutoRefresh: (Boolean) -> Unit,
    onRefreshNetwork: () -> Unit,
    onSelectBand: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val cellInfo = uiState.cellInfo
    val location = uiState.location
    val site = uiState.matchedSite

    val timeFormat = remember { java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()) }
    val formattedLastRefresh = remember(uiState.lastRefreshTimeMs) {
        timeFormat.format(java.util.Date(uiState.lastRefreshTimeMs))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Voice Alert Bar
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isVoiceEnabled) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (uiState.isVoiceEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Voice Alert Icon",
                            tint = if (uiState.isVoiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (uiState.isVoiceEnabled) "Voice Notifications Active" else "Voice Notifications Muted",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Announces Site, Sector & Band",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(
                            onClick = onTestVoiceAlert,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "Re-announce Voice",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Switch(
                            checked = uiState.isVoiceEnabled,
                            onCheckedChange = onToggleVoiceAlerts
                        )
                    }
                }
            }
        }

        // Network Refresh Control Card (Auto 5s vs Manual)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Network Refresh",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "NETWORK REFRESH MODE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = if (uiState.isAutoRefresh) "Auto (Refreshes every 15 sec)" else "Manual (On-Demand Refresh)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Text(
                            text = if (uiState.refreshCount > 0) "#${uiState.refreshCount} • $formattedLastRefresh" else "Updated $formattedLastRefresh",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Auto Mode Chip
                        FilterChip(
                            selected = uiState.isAutoRefresh,
                            onClick = { onToggleAutoRefresh(true) },
                            label = { Text("Auto (15s)", fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = "Auto Refresh 15s",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Manual Mode Chip
                        FilterChip(
                            selected = !uiState.isAutoRefresh,
                            onClick = { onToggleAutoRefresh(false) },
                            label = { Text("Manual", fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = "Manual Refresh",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Refresh Now Button
                        Button(
                            onClick = onRefreshNetwork,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Now",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refresh Now", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Refresh Notification Alert Banner
        if (uiState.lastRefreshNotification.isNotBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Refresh Notification",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "REFRESH NOTIFICATION SENT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = uiState.lastRefreshNotification,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Serving Cell Header Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = cellInfo.technology,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Button(
                            onClick = onTestVoiceAlert,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Announce Voice",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Speak Telemetry", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "SITE ID",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = cellInfo.siteId,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            site?.let {
                                Text(
                                    text = it.siteName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "SECTOR ID",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = cellInfo.sectorId,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            uiState.matchedSector?.let {
                                Text(
                                    text = "Azimuth: ${it.azimuth.toInt()}°",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary info row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoDetail("TAC", cellInfo.tac.toString())
                        InfoDetail("PCI", cellInfo.pci.toString())
                        InfoDetail("EARFCN", cellInfo.earfcn.toString())
                        InfoDetail("MCC-MNC", cellInfo.mccMnc)
                    }
                }
            }
        }

        // Distance & Compass Card
        item {
            DistanceCompassCard(
                distanceFormatted = DistanceCalculator.formatDistance(uiState.distanceToSiteMeters),
                bearingDegrees = uiState.bearingToSiteDegrees,
                siteName = site?.siteName ?: "Site ${cellInfo.siteId}",
                sectorName = uiState.matchedSector?.sectorName ?: "Sector ${cellInfo.sectorId}"
            )
        }

        // Frequency Bands Row
        item {
            Column {
                Text(
                    text = "ACTIVE FREQUENCY BANDS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    val bands = listOf(
                        Pair("B20 (800 MHz)", 6200),
                        Pair("B8 (900 MHz)", 3500),
                        Pair("B3 (1800 MHz)", 1300),
                        Pair("B1 (2100 MHz)", 300),
                        Pair("B7 (2600 MHz)", 2850)
                    )
                    items(bands) { (label, earfcn) ->
                        val isCurrent = cellInfo.frequencyLabel.contains(label.take(4)) || cellInfo.bandName.contains(label.take(4))
                        BandChip(
                            bandLabel = label,
                            isSelected = isCurrent,
                            onClick = { onSelectBand(earfcn) }
                        )
                    }
                }
            }
        }

        // RF Signal Gauge Card
        item {
            SignalGaugeCard(cellInfo = cellInfo)
        }

        // GPS Location Details Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "GPS Location",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "CURRENT GPS LOCATION",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (location.isValid) "GPS Lock Acquired" else "Acquiring GPS Fix...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (location.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Latitude", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                String.format("%.6f", location.latitude),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column {
                            Text("Longitude", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                String.format("%.6f", location.longitude),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column {
                            Text("Accuracy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${location.accuracyMeters.toInt()} m",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoDetail(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
