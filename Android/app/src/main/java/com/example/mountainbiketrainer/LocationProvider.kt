package com.example.mountainbiketrainer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow


class LocationProvider(context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val context = context

    @SuppressLint("MissingPermission") // Ensure permissions are handled before calling
    fun locationUpdatesFlow(): Flow<TimestampedSensorEvent> = callbackFlow {

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            System.err.println("FusedLocationProvider: Attempted to start updates without permission.")
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            MIN_TIME_BW_UPDATES_MS
        )
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_CHANGE_FOR_UPDATES_METERS)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    trySend(
                        GPSSpeedEvent(
                            timestamp = location.time, // Milliseconds
                            speedMps = location.speed,
                            accuracyMps = if (location.hasSpeedAccuracy()) location.speedAccuracyMetersPerSecond else null
                        )
                    ).isSuccess
                    trySend(
                        GPSLocationEvent(
                            timestamp = location.time,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitude = if (location.hasAltitude()) location.altitude else null,
                            accuracyHorizontal = if (location.hasAccuracy()) location.accuracy else null
                        )
                    ).isSuccess
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
    companion object {
        private const val MIN_TIME_BW_UPDATES_MS: Long = 1000 // 1 second (desired interval)
        private const val MIN_UPDATE_INTERVAL_MS: Long = 500 // Smallest possible interval
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES_METERS: Float = 0f // Update on any movement
    }
}