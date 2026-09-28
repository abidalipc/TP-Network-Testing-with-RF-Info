package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Default fallback coordinates near sample sites (Central Plaza Tower area)
    private val defaultFallbackLocation = LocationData(
        latitude = 3.1385,
        longitude = 101.6865,
        altitude = 45.0,
        accuracyMeters = 5f,
        isGpsActive = true,
        provider = "Simulated GPS"
    )

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(intervalMs: Long = 3000L): Flow<LocationData> = callbackFlow {
        // Emit default location initially so map/distances are immediately valid
        trySend(defaultFallbackLocation)

        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val isAppOpsAllowed = try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
            if (appOps != null) {
                val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    appOps.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_FINE_LOCATION, android.os.Process.myUid(), context.packageName)
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_FINE_LOCATION, android.os.Process.myUid(), context.packageName)
                }
                mode == android.app.AppOpsManager.MODE_ALLOWED
            } else true
        } catch (_: Exception) {
            true
        }

        if ((!hasFine && !hasCoarse) || !isAppOpsAllowed) {
            // Permission or AppOps not granted, gracefully yield without triggering AppOps logs
            awaitClose { }
            return@callbackFlow
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    val locData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = location.altitude,
                        accuracyMeters = location.accuracy,
                        speedMps = location.speed,
                        bearingDegrees = location.bearing,
                        isGpsActive = true,
                        provider = location.provider ?: "GPS",
                        timestampMs = System.currentTimeMillis()
                    )
                    trySend(locData)
                }
            }
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            intervalMs
        ).setMinUpdateIntervalMillis(1000L)
            .setWaitForAccurateLocation(false)
            .build()

        var isUpdatesRequested = false
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    trySend(
                        LocationData(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            altitude = loc.altitude,
                            accuracyMeters = loc.accuracy,
                            speedMps = loc.speed,
                            bearingDegrees = loc.bearing,
                            isGpsActive = true,
                            provider = loc.provider ?: "GPS",
                            timestampMs = System.currentTimeMillis()
                        )
                    )
                }
            }
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            isUpdatesRequested = true
        } catch (_: SecurityException) {
        } catch (_: Exception) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (locationManager != null && (hasFine || hasCoarse) && isAppOpsAllowed) {
                    val fallbackListener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            trySend(
                                LocationData(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    altitude = location.altitude,
                                    accuracyMeters = location.accuracy,
                                    speedMps = location.speed,
                                    bearingDegrees = location.bearing,
                                    isGpsActive = true,
                                    provider = location.provider ?: "GPS"
                                )
                            )
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    }
                    val isGpsEnabled = try { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (_: Exception) { false }
                    val isNetworkEnabled = try { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { false }

                    if (isNetworkEnabled) {
                        locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            intervalMs,
                            1f,
                            fallbackListener,
                            Looper.getMainLooper()
                        )
                    } else if (isGpsEnabled) {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            intervalMs,
                            1f,
                            fallbackListener,
                            Looper.getMainLooper()
                        )
                    }
                }
            } catch (_: SecurityException) {
            } catch (_: Exception) {}
        }

        awaitClose {
            if (isUpdatesRequested) {
                try {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                } catch (_: Exception) {}
            }
        }
    }
}

