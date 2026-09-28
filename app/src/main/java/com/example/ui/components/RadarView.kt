package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.SectorEntity
import com.example.data.db.SiteEntity
import com.example.location.DistanceCalculator
import com.example.location.LocationData
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CellRadarView(
    userLocation: LocationData,
    sites: List<SiteEntity>,
    sectorsMap: Map<String, List<SectorEntity>>,
    activeSiteId: String,
    activeSectorId: String,
    onSiteSelected: (SiteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var isMapMode by remember { mutableStateOf(true) }

    // Rotating radar sweep animation
    val infiniteTransition = rememberInfiniteTransition(label = "radarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "sweepAngle"
    )

    // Pulse animation for user location
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    val maxSiteDist = remember(sites, userLocation) {
        sites.maxOfOrNull {
            DistanceCalculator.calculateDistanceMeters(
                userLocation.latitude, userLocation.longitude,
                it.latitude, it.longitude
            )
        } ?: 1000.0
    }
    val maxRadarDistanceMeters = remember(maxSiteDist) {
        maxOf(1000.0, Math.ceil(maxSiteDist / 500.0) * 500.0)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(390.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMapMode) Color(0xFF020617) else Color(0xFF0F172A)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(sites, userLocation, maxRadarDistanceMeters) {
                        detectTapGestures { tapOffset ->
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val radarRadiusPx = size.width / 2f - 30f
                            val scalePixelsPerMeter = radarRadiusPx / maxRadarDistanceMeters

                            sites.forEach { site ->
                                val dist = DistanceCalculator.calculateDistanceMeters(
                                    userLocation.latitude, userLocation.longitude,
                                    site.latitude, site.longitude
                                )
                                val bearing = DistanceCalculator.calculateBearing(
                                    userLocation.latitude, userLocation.longitude,
                                    site.latitude, site.longitude
                                )
                                val bearingRad = Math.toRadians((bearing - 90).toDouble())
                                val siteDistPx = (dist * scalePixelsPerMeter).toFloat().coerceAtMost(radarRadiusPx)
                                val siteX = centerX + (siteDistPx * cos(bearingRad)).toFloat()
                                val siteY = centerY + (siteDistPx * sin(bearingRad)).toFloat()

                                val dx = tapOffset.x - siteX
                                val dy = tapOffset.y - siteY
                                if (dx * dx + dy * dy < 50f * 50f) {
                                    onSiteSelected(site)
                                    return@detectTapGestures
                                }
                            }
                        }
                    }
            ) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val radarRadiusPx = size.width / 2f - 30f
                val scalePixelsPerMeter = radarRadiusPx / maxRadarDistanceMeters

                if (isMapMode) {
                    // Map Grid background with lat/lng grid lines
                    val stepPx = 60f
                    var x = 0f
                    while (x < size.width) {
                        drawLine(
                            color = Color(0xFF1E293B).copy(alpha = 0.4f),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                        x += stepPx
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = Color(0xFF1E293B).copy(alpha = 0.4f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += stepPx
                    }
                }

                // 1. Draw Radar Grid Rings
                val ringCount = 4
                val stepMeters = maxRadarDistanceMeters / ringCount
                for (i in 1..ringCount) {
                    val ringMeters = stepMeters * i
                    val ringPx = (ringMeters * scalePixelsPerMeter).toFloat()
                    if (ringPx <= radarRadiusPx) {
                        drawCircle(
                            color = if (isMapMode) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFF334155),
                            radius = ringPx,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 1.2f)
                        )
                    }
                }

                // Crosshair axes
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(centerX, centerY - radarRadiusPx),
                    end = Offset(centerX, centerY + radarRadiusPx),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(centerX - radarRadiusPx, centerY),
                    end = Offset(centerX + radarRadiusPx, centerY),
                    strokeWidth = 1.5f
                )

                // Cardinal Directions N, E, S, W
                val cardinalPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#38BDF8")
                    textSize = 28f
                    isFakeBoldText = true
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawContext.canvas.nativeCanvas.drawText("N", centerX, centerY - radarRadiusPx + 24f, cardinalPaint)
                drawContext.canvas.nativeCanvas.drawText("S", centerX, centerY + radarRadiusPx - 10f, cardinalPaint)
                drawContext.canvas.nativeCanvas.drawText("E", centerX + radarRadiusPx - 16f, centerY + 10f, cardinalPaint)
                drawContext.canvas.nativeCanvas.drawText("W", centerX - radarRadiusPx + 16f, centerY + 10f, cardinalPaint)

                // 2. Radar Sweep Line (Tactical Mode)
                if (!isMapMode) {
                    rotate(sweepAngle, pivot = Offset(centerX, centerY)) {
                        drawLine(
                            color = Color(0xFF0EA5E9).copy(alpha = 0.5f),
                            start = Offset(centerX, centerY),
                            end = Offset(centerX + radarRadiusPx, centerY),
                            strokeWidth = 2.5f
                        )
                    }
                }

                // 3. Plot Cell Sites, Sectors & Distance Lines
                sites.forEach { site ->
                    val dist = DistanceCalculator.calculateDistanceMeters(
                        userLocation.latitude, userLocation.longitude,
                        site.latitude, site.longitude
                    )
                    val bearing = DistanceCalculator.calculateBearing(
                        userLocation.latitude, userLocation.longitude,
                        site.latitude, site.longitude
                    )

                    val bearingRad = Math.toRadians((bearing - 90).toDouble())
                    val siteDistPx = (dist * scalePixelsPerMeter).toFloat().coerceAtMost(radarRadiusPx)
                    val siteX = centerX + siteDistPx * cos(bearingRad).toFloat()
                    val siteY = centerY + siteDistPx * sin(bearingRad).toFloat()

                    val isActiveSite = site.siteId == activeSiteId

                    // Distance Line connecting User Location to Site
                    drawLine(
                        color = if (isActiveSite) Color(0xFF10B981) else Color(0xFF38BDF8).copy(alpha = 0.4f),
                        start = Offset(centerX, centerY),
                        end = Offset(siteX, siteY),
                        strokeWidth = if (isActiveSite) 2.5f else 1.2f
                    )

                    // Draw Sector Beams Cones
                    val siteSectors = sectorsMap[site.siteId] ?: emptyList()
                    siteSectors.forEach { sector ->
                        val azRad = Math.toRadians((sector.azimuth - 90).toDouble())
                        val halfBeamRad = Math.toRadians((sector.beamwidth / 2f).toDouble())
                        val beamLength = 45f

                        val path = Path().apply {
                            moveTo(siteX, siteY)
                            lineTo(
                                siteX + beamLength * cos(azRad - halfBeamRad).toFloat(),
                                siteY + beamLength * sin(azRad - halfBeamRad).toFloat()
                            )
                            lineTo(
                                siteX + beamLength * cos(azRad + halfBeamRad).toFloat(),
                                siteY + beamLength * sin(azRad + halfBeamRad).toFloat()
                            )
                            close()
                        }

                        val isActiveSector = isActiveSite && sector.sectorId == activeSectorId
                        val sectorColor = if (isActiveSector) Color(0xFF10B981) else Color(0xFF38BDF8)

                        drawPath(
                            path = path,
                            color = sectorColor.copy(alpha = if (isActiveSector) 0.5f else 0.2f)
                        )
                    }

                    // Draw Site Point Pin
                    val pointColor = if (isActiveSite) Color(0xFF10B981) else Color(0xFF0EA5E9)
                    drawCircle(
                        color = pointColor,
                        radius = if (isActiveSite) 11f else 7f,
                        center = Offset(siteX, siteY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = Offset(siteX, siteY)
                    )

                    // Draw Native Canvas Text for Site ID & Distance Tag
                    val distFormatted = DistanceCalculator.formatDistance(dist)
                    val siteLabel = "Site ${site.siteId} (${distFormatted})"
                    
                    val paint = android.graphics.Paint().apply {
                        color = if (isActiveSite) android.graphics.Color.parseColor("#10B981") else android.graphics.Color.parseColor("#E2E8F0")
                        textSize = 26f
                        isFakeBoldText = isActiveSite
                        isAntiAlias = true
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        siteLabel,
                        siteX + 14f,
                        siteY + 8f,
                        paint
                    )
                }

                // 4. Draw Center User Location Pulse
                drawCircle(
                    color = Color(0xFF0EA5E9).copy(alpha = (1f - (pulseScale - 10f) / 18f).coerceIn(0f, 1f)),
                    radius = pulseScale,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = Color(0xFF0EA5E9),
                    radius = 9f,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.5f,
                    center = Offset(centerX, centerY)
                )
            }

            // Radar/Map Header overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isMapMode) "GOOGLE SITE MAP" else "TACTICAL SECTOR RADAR",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "User Lat: ${String.format("%.4f", userLocation.latitude)}, Lng: ${String.format("%.4f", userLocation.longitude)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                FilledTonalIconButton(
                    onClick = { isMapMode = !isMapMode },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color(0xFF38BDF8)
                    )
                ) {
                    Icon(
                        imageVector = if (isMapMode) Icons.Default.Radar else Icons.Default.Map,
                        contentDescription = "Toggle View Mode"
                    )
                }
            }
        }
    }
}

