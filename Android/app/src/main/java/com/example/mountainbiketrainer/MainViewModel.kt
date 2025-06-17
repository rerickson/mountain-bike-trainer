package com.example.mountainbiketrainer

import android.app.Application
import android.hardware.SensorManager
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

data class SaveFileRequest(val suggestedName: String, val dataToSave: List<TimestampedSensorEvent>)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val sensorDataProvider = SensorDataProvider(application.applicationContext)
    private val locationProvider = LocationProvider(application.applicationContext)

    private var allRecordedEvents = mutableListOf<TimestampedSensorEvent>()
    private var sensorCollectionJob: Job? = null

    private val _locationPermissionGranted = MutableStateFlow(true)
    private val _collecting = MutableStateFlow(false)

    private val _currentSpeed = MutableStateFlow<GPSSpeedEvent?>(null)
    val currentSpeed: StateFlow<GPSSpeedEvent?> = _currentSpeed.asStateFlow()

    private val _maxSpeed = MutableStateFlow<GPSSpeedEvent?>(null)
    val maxSpeed: StateFlow<GPSSpeedEvent?> = _maxSpeed.asStateFlow()

    private val _maxGForce = MutableStateFlow<Float?>(null)
    val maxGForce: StateFlow<Float?> = _maxGForce.asStateFlow()

    private val jumpDetector = JumpDetector()

    private val _lastAirTime = MutableStateFlow<Float?>(null)
    val lastAirTime: StateFlow<Float?> = _lastAirTime.asStateFlow()

    private val _saveFileRequestChannel = Channel<SaveFileRequest>()
    val saveFileRequestFlow = _saveFileRequestChannel.receiveAsFlow()

    private val _currentLinearAccel = MutableStateFlow<LinearAccelEvent?>(null)
    val currentLinearAccel: StateFlow<LinearAccelEvent?> = _currentLinearAccel.asStateFlow()

    private val _maxLinearAccel = MutableStateFlow<LinearAccelEvent?>(null)
    val maxLinearAccel: StateFlow<LinearAccelEvent?> = _maxLinearAccel.asStateFlow()

    fun onLocationPermissionsGranted() {
        _locationPermissionGranted.value = true
    }

    fun getCollecting(): Boolean {
        return _collecting.value
    }

    fun toggleOverallDataCollection() {
        _collecting.value = !_collecting.value
        if (_collecting.value) {
            sensorCollectionJob = viewModelScope.launch {
                val sensorFlow = sensorDataProvider.sensorEventsFlow()
                val locationFlow = locationProvider.locationUpdatesFlow()

                merge(sensorFlow, locationFlow) // Combine all event sources
                    .collect { event ->
                        if (_collecting.value) {
                            allRecordedEvents.add(event) // Store the event for processing later

                            // Add minimal processing to update UI
                            if(event is AccelerometerEvent) {
                                val airTime = jumpDetector.processSensorEvent(event)
                                if (airTime != null) {
                                    _lastAirTime.value = airTime
                                }
                            }
                            if(event is GPSSpeedEvent) {
                                _currentSpeed.value = event
                                if (_maxSpeed.value == null || event.speedMps > _maxSpeed.value!!.speedMps) {
                                    _maxSpeed.value = event
                                }
                            }
                            if(event is LinearAccelEvent) {
                                val magnitudeMs2 = sqrt(event.x * event.x + event.y * event.y + event.z * event.z)
                                val gforce = magnitudeMs2/ SensorManager.GRAVITY_EARTH
                                if (_maxGForce.value == null || gforce > _maxGForce.value!!) {
                                    _maxGForce.value = gforce
                                }
                                _currentLinearAccel.value = event
                                if (_maxLinearAccel.value == null || event.z > _maxLinearAccel.value!!.z) {
                                    _maxLinearAccel.value = event
                                }
                            }
                        }
                    }
            }
            Log.i("MainViewModel","Started data collection for all events.")
        } else {
            sensorCollectionJob?.cancel()
            sensorCollectionJob = null
            Log.i("MainViewModel","Stopped data collection.")

            if (allRecordedEvents.isEmpty()) {
                Log.i("MainViewModel", "No sensor events to export.")
                return
            }

            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val suggestedFileName = "session_raw_all_${sdf.format(Date())}.json"

            viewModelScope.launch {
                // Logcat for debugging
                 allRecordedEvents.takeLast(100).forEach { Log.d("MainViewModel", "Event: $it") }

                _saveFileRequestChannel.send(
                    SaveFileRequest(
                        suggestedName = suggestedFileName,
                        dataToSave = ArrayList(allRecordedEvents)
                    )
                )
            }
        }
    }

    fun writeDataToUri(uri: Uri, data: List<TimestampedSensorEvent>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

                    mapper.enable(SerializationFeature.INDENT_OUTPUT)
                    // Build the list of objects as you were
                    val jsonString: String = mapper.writeValueAsString(data)
                    outputStream.write(jsonString.toByteArray())
                    Log.i("MainViewModel", "All sensor event data saved to URI: $uri")
                    // allRecordedEvents.clear() // Clear only if this is the definitive end of data
                } ?: run {
                    Log.e("MainViewModel", "Failed to open OutputStream for URI: $uri")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error saving data to URI", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorCollectionJob?.cancel()
    }

    fun resetMax() {
        _maxSpeed.value = null
        _currentSpeed.value = null
        _lastAirTime.value = null
    }
}