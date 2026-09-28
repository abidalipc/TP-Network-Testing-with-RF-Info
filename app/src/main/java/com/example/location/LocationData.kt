package com.example.location

import kotlin.math.*

/**
 * Current GPS / Location state.
 */
data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val accuracyMeters: Float = 0f,
    val speedMps: Float = 0f,
    val bearingDegrees: Float = 0f,
    val isGpsActive: Boolean = false,
    val provider: String = "GPS",
    val timestampMs: Long = System.currentTimeMillis()
) {
    val isValid: Boolean get() = latitude != 0.0 && longitude != 0.0
}

object DistanceCalculator {

    /**
     * Calculates distance in meters between two lat/lng coordinates using Haversine formula.
     */
    fun calculateDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadiusMeters = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusMeters * c
    }

    /**
     * Calculates bearing / azimuth in degrees (0..360) from point 1 to point 2.
     */
    fun calculateBearing(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        var bearing = Math.toDegrees(atan2(y, x)).toFloat()
        if (bearing < 0) bearing += 360f
        return bearing
    }

    fun formatDistance(meters: Double): String {
        return if (meters >= 1000.0) {
            String.format("%.2f km", meters / 1000.0)
        } else {
            String.format("%.0f m", meters)
        }
    }
}
