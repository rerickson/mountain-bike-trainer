package com.example.mountainbiketrainer

import android.Manifest
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SpeedData(val speedMph: Float)

class LocationProvider(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallbackInstance: LocationCallback? = null

    private val _speedDataFlow = MutableStateFlow<SpeedData?>(null)
    val locationUpdates: Flow<SpeedData?> = _speedDataFlow.asStateFlow()

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        MIN_TIME_BW_UPDATES_MS
    )
        .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
        .setMinUpdateDistanceMeters(MIN_DISTANCE_CHANGE_FOR_UPDATES_METERS)
        .build()

    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            System.err.println("FusedLocationProvider: Attempted to start updates without permission.")
            _speedDataFlow.value = null
            return
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    if (location.hasSpeed()) {
                        val speedMph = location.speed * 2.23694f
                        println("FusedLocationProvider: Speed available - $speedMph MPH")
                        _speedDataFlow.value = SpeedData(speedMph)
                    } else {
                         _speedDataFlow.value = null
                        println("FusedLocationProvider: No speed data.")
                    }
                }
            }
        }
        locationCallbackInstance = callback

        println("FusedLocationProvider: Requesting location updates...")
        fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
            .addOnFailureListener { e ->
                println("FusedLocationProvider: FAILED to request location updates: ${e.message}")
                _speedDataFlow.value = null // Emit null or error state
            }
            .addOnSuccessListener {
                println("FusedLocationProvider: Successfully started location updates.")
            }
        println("FusedLocationProvider: Started location updates flow setup.")
    }

    fun stopLocationUpdates() {
        locationCallbackInstance?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallbackInstance = null
            println("FusedLocationProvider: Stopped location updates.")
        }
        _speedDataFlow.value = null // Clear last known speed
    }

    companion object {
        private const val MIN_TIME_BW_UPDATES_MS: Long = 1000 // 1 second (desired interval)
        private const val MIN_UPDATE_INTERVAL_MS: Long = 500 // Smallest possible interval
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES_METERS: Float = 0f // Update on any movement
    }
}