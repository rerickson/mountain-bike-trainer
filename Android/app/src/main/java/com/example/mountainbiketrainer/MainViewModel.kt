package com.example.mountainbiketrainer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

@Suppress("OPT_IN_USAGE")
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val sensorDataProvider = SensorDataProvider(application.applicationContext)
    private val locationProvider = LocationProvider(application.applicationContext)
    private val _locationPermissionGranted = MutableStateFlow(true)
    private val _collecting = MutableStateFlow(false)

    private val _currentSpeed = MutableStateFlow<SpeedData?>(null)
    val currentSpeed: StateFlow<SpeedData?> = _currentSpeed.asStateFlow()

    private val _maxSpeed = MutableStateFlow<SpeedData?>(null)
    val maxSpeed: StateFlow<SpeedData?> = _maxSpeed.asStateFlow()

    val processedSensorData: StateFlow<ProcessedSensorData> = sensorDataProvider.processedData

    init {
        _locationPermissionGranted.flatMapLatest { granted ->
            if (granted) {
                locationProvider.startLocationUpdates()
                locationProvider.locationUpdates
                    .onEach { newSpeedData ->
                        _currentSpeed.value = newSpeedData

                        newSpeedData?.let { current ->
                            _maxSpeed.update { oldMax ->
                                if (oldMax == null || current.speedMph > oldMax.speedMph) {
                                    println("MainViewModel: New Max Speed - ${current.speedMph} MPH")
                                    current
                                } else {
                                    oldMax
                                }
                            }
                        }
                    }
                    .catch { e ->
                        System.err.println("MainViewModel: Error in Fused location updates flow: ${e.message}")
                        _currentSpeed.value = null
                    }
            } else {
                locationProvider.stopLocationUpdates()
                _currentSpeed.value = null
                MutableStateFlow<SpeedData?>(null)
            }
        }.launchIn(viewModelScope) // Collect the flow within the ViewModel's scope
    }

    fun onLocationPermissionsGranted() {
        _locationPermissionGranted.value = true
    }

    fun getCollecting(): Boolean {
        return _collecting.value
    }

    fun toggleOverallDataCollection() {
        _collecting.value = !_collecting.value
        if (_collecting.value) {
            sensorDataProvider.startDataCollection()
            println("ViewModel: Started data collection (sensors + location if permitted).")
        } else {
            sensorDataProvider.stopDataCollection()
            println("ViewModel: Stopped data collection.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorDataProvider.cleanup()
        locationProvider.stopLocationUpdates()
    }

    fun resetMax() {
        sensorDataProvider.resetMax()
        _maxSpeed.value = null
    }
}