package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.UiState

@Composable
fun SettingsScreen(
    uiState: UiState,
    onToggleVoiceAlerts: (Boolean) -> Unit,
    onTestVoiceAlert: () -> Unit,
    onToggleSimulationMode: (Boolean) -> Unit,
    onSelectSimulationBand: (Int, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var simSiteInput by remember { mutableStateOf(uiState.cellInfo.siteId.ifBlank { "104" }) }
    var simSectorInput by remember { mutableStateOf(uiState.cellInfo.sectorId.ifBlank { "2" }) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Voice Notification Settings
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Voice Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "VOICE NOTIFICATIONS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Switch(
                        checked = uiState.isVoiceEnabled,
                        onCheckedChange = onToggleVoiceAlerts
                    )
                }

                Text(
                    text = "Text-To-Speech engine automatically speaks when connected Site ID or Sector ID changes during field telemetry.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onTestVoiceAlert,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = "Test Voice")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Voice Notification")
                }
            }
        }

        // Simulation / Override Control
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Simulation",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "FIELD SIMULATION MODE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Switch(
                        checked = uiState.isSimulationMode,
                        onCheckedChange = onToggleSimulationMode
                    )
                }

                Text(
                    text = "Simulate cellular site & sector handovers across 800, 900, 1800, 2100 & 2600 MHz bands for testing without SIM card access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (uiState.isSimulationMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = simSiteInput,
                            onValueChange = { simSiteInput = it },
                            label = { Text("Site ID") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = simSectorInput,
                            onValueChange = { simSectorInput = it },
                            label = { Text("Sector ID") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Text(
                        text = "Select Target Frequency Band:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val simBands = listOf(
                        Pair("800 MHz (B20)", 6200),
                        Pair("900 MHz (B8)", 3500),
                        Pair("1800 MHz (B3)", 1300),
                        Pair("2100 MHz (B1)", 300),
                        Pair("2600 MHz (B7)", 2850)
                    )

                    simBands.forEach { (label, earfcn) ->
                        OutlinedButton(
                            onClick = { onSelectSimulationBand(earfcn, simSiteInput, simSectorInput) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Switch to $label")
                        }
                    }
                }
            }
        }

        // OS & Device Compatibility Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "OS Info",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "SYSTEM COMPATIBILITY",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                CompatRow("Android Version", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                CompatRow("OxygenOS / OEM Theme", "Seamless M3 Material You Dynamic Palette")
                CompatRow("Edge-to-Edge Support", "Enabled (WindowInsets Safe Drawing)")
                CompatRow("Telephony API", "TelephonyManager & CellInfo Listening Active")
                CompatRow("GPS Location API", "FusedLocationProviderClient Active")
            }
        }
    }
}

@Composable
fun CompatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
