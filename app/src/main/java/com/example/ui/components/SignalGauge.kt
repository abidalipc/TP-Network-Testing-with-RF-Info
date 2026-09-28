package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telecom.CellInfoData
import com.example.telecom.SignalQuality
import com.example.ui.theme.*

@Composable
fun SignalGaugeCard(
    cellInfo: CellInfoData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SignalCellular4Bar,
                            contentDescription = "Signal Icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "RF SIGNAL STRENGTH",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${cellInfo.rsrp} dBm (RSRP)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Quality Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(cellInfo.signalQuality.colorHex).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = cellInfo.signalQuality.label,
                        color = Color(cellInfo.signalQuality.colorHex),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // RSRP Bar
            val rsrpProgress = ((cellInfo.rsrp + 140).coerceIn(0, 100) / 100f)
            val animatedRsrp by animateFloatAsState(
                targetValue = rsrpProgress,
                animationSpec = tween(durationMillis = 500),
                label = "rsrpAnim"
            )

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RSRP (-140 to -40 dBm)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${cellInfo.rsrp} dBm",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(cellInfo.signalQuality.colorHex)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { animatedRsrp },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = Color(cellInfo.signalQuality.colorHex),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary metrics row (RSRQ, RSSI, SINR)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricPill("RSRQ", "${cellInfo.rsrq} dB")
                MetricPill("RSSI", "${cellInfo.rssi} dBm")
                MetricPill("SINR", "${cellInfo.sinr} dB")
            }
        }
    }
}

@Composable
fun MetricPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
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
}

@Composable
fun BandChip(
    bandLabel: String,
    isSelected: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val chipColor = when {
        bandLabel.contains("800") -> Band800Color
        bandLabel.contains("900") -> Band900Color
        bandLabel.contains("1800") -> Band1800Color
        bandLabel.contains("2100") -> Band2100Color
        bandLabel.contains("2600") -> Band2600Color
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) chipColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, chipColor) else null,
        modifier = Modifier.padding(end = 6.dp)
    ) {
        Text(
            text = bandLabel,
            color = if (isSelected) chipColor else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun DistanceCompassCard(
    distanceFormatted: String,
    bearingDegrees: Float,
    siteName: String,
    sectorName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DISTANCE TO TOWER",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = distanceFormatted,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$siteName • $sectorName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Compass Needle Circle
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Bearing Arrow",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(36.dp)
                        .rotate(bearingDegrees)
                )
            }
        }
    }
}
