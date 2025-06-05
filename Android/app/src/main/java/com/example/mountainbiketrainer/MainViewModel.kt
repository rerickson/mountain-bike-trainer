package com.example.mountainbiketrainer

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private val jumpDetector = JumpDetector()

    private val _lastAirTime = MutableStateFlow<Float?>(null)
    val lastAirTime: StateFlow<Float?> = _lastAirTime.asStateFlow()

    private val recordedAccelData = mutableListOf<AccelerometerData>()

    init {
        _locationPermissionGranted.flatMapLatest { granted ->
            if (granted) {
                locationProvider.startLocationUpdates()
                locationProvider.locationUpdates
                    .onEach { newSpeedData ->
                        if(!_collecting.value) return@onEach

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
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            processedSensorData
                .filterNotNull()
                .collect { data ->
                    if (!_collecting.value) return@collect

                    data.accelerometerData?.let {
                        recordedAccelData.add(it)
                    }
                     val airTime = jumpDetector.processSensorEvent(data)
                     airTime?.let {
                        _lastAirTime.value = it
                     }
                }
        }
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
            exportRecordedAccelDataToLogcat()
            println("ViewModel: Stopped data collection.")
        }
    }
    private fun exportRecordedAccelDataToLogcat() {
        if (recordedAccelData.isEmpty()) {
            Log.i("MainViewModel", "No accelerometer data to export.")
            return
        }
        Log.i("MainViewModel", "--- ACCELEROMETER DATA (CSV Format) ---")
        Log.i("ACCEL_DATA_CSV", "timestamp_ns,accel_x,accel_y,accel_z") // Header
        recordedAccelData.forEach { data ->
            // Logcat has line length limits, so for very long recordings, file export is better
            Log.d("ACCEL_DATA_CSV", "${data.timestamp},${data.x},${data.y},${data.z}")
            // To find and replace the log prefix use this regex:
            // ^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d{3,}\s+\d+-\d+\s+ACCEL_DATA_CSV\s+com\.example\.mountainbiketrainer\s+[A-Z]\s+
        }
        Log.i("MainViewModel", "--- END OF ACCELEROMETER DATA ---")
         recordedAccelData.clear()
    }

    override fun onCleared() {
        super.onCleared()
        sensorDataProvider.cleanup()
        locationProvider.stopLocationUpdates()
    }

    fun resetMax() {
        sensorDataProvider.resetMax()
        _maxSpeed.value = null
        _currentSpeed.value = null
        _lastAirTime.value = null
    }
}