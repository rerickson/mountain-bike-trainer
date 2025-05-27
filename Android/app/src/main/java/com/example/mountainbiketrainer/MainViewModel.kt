package com.example.mountainbiketrainer

import LocationProvider
import SpeedData
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val sensorDataProvider = SensorDataProvider(application.applicationContext)
    private val locationProvider = LocationProvider(application.applicationContext)
    private val _locationPermissionGranted = MutableStateFlow(false)
    private val _collecting = MutableStateFlow(false)

    // Expose the StateFlow from the provider
    val processedSensorData: StateFlow<ProcessedSensorData> = sensorDataProvider.processedData
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSpeed: StateFlow<SpeedData?> = _locationPermissionGranted.flatMapLatest { granted ->
        if (granted) {
            locationProvider.startLocationUpdates()
            locationProvider.locationUpdates.catch { e ->
                // Handle errors from the locationUpdates flow (e.g., provider disabled)
                System.err.println("Error in location updates flow: ${e.message}")
                emit(SpeedData(speedMph = -1f))
            }
        } else {
            locationProvider.stopLocationUpdates() // Ensure it's stopped if permission revoked
            MutableStateFlow<SpeedData?>(null) // Emit null or an empty flow if not granted
        }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun onLocationPermissionsGranted() {
        _locationPermissionGranted.value = true
    }

    // Call this if permissions are revoked or explicitly denied by the user
    fun onLocationPermissionsDenied() {
        _locationPermissionGranted.value = false
        locationProvider.stopLocationUpdates() // Explicitly stop
    }

    fun getCollecting(): Boolean {
        return _collecting.value;
    }

    fun toggleOverallDataCollection() {
        if (_locationPermissionGranted.value) { // Only proceed if permission is there
            _collecting.value = !_collecting.value
            if (_collecting.value) {
                sensorDataProvider.startDataCollection()
                // Location updates are handled by the 'currentSpeed' flow based on permission state
                println("ViewModel: Started data collection (sensors + location if permitted).")
            } else {
                sensorDataProvider.stopDataCollection()

                println("ViewModel: Stopped data collection.")
            }
        } else {
            println("ViewModel: Cannot start data collection, location permission not granted.")
        }
    }


    // It's important to clean up the SensorDataProvider when the ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        sensorDataProvider.cleanup()
        locationProvider.stopLocationUpdates()
    }

    fun resetMax() {
        sensorDataProvider.resetMax()
    }
}