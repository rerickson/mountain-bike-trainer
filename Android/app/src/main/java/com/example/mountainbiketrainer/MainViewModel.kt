package com.example.mountainbiketrainer

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class SaveFileRequest(val suggestedName: String, val dataToSave: List<TimestampedSensorEvent>)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private var sensorDataService: SensorDataService? = null
    private var bound = false

    private val _locationPermissionGranted = MutableStateFlow(true)
    private val _collecting = MutableStateFlow(false)

    // Delegate to service state flows
    private val _currentSpeed = MutableStateFlow<GPSSpeedEvent?>(null)
    val currentSpeed: StateFlow<GPSSpeedEvent?> = _currentSpeed.asStateFlow()
    
    private val _maxSpeed = MutableStateFlow<GPSSpeedEvent?>(null)
    val maxSpeed: StateFlow<GPSSpeedEvent?> = _maxSpeed.asStateFlow()
    
    private val _maxGForce = MutableStateFlow<Float?>(null)
    val maxGForce: StateFlow<Float?> = _maxGForce.asStateFlow()
    
    private val _lastAirTime = MutableStateFlow<Float?>(null)
    val lastAirTime: StateFlow<Float?> = _lastAirTime.asStateFlow()
    
    private val _currentLinearAccel = MutableStateFlow<LinearAccelEvent?>(null)
    val currentLinearAccel: StateFlow<LinearAccelEvent?> = _currentLinearAccel.asStateFlow()
    
    private val _maxLinearAccel = MutableStateFlow<LinearAccelEvent?>(null)
    val maxLinearAccel: StateFlow<LinearAccelEvent?> = _maxLinearAccel.asStateFlow()
    
    private val _saveFileRequestFlow = MutableStateFlow<SaveFileRequest?>(null)
    val saveFileRequestFlow: StateFlow<SaveFileRequest?> = _saveFileRequestFlow.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as SensorDataService.LocalBinder
            sensorDataService = binder.getService()
            bound = true
            Log.d("MainViewModel", "Service connected")
            
            // Observe service state flows
            viewModelScope.launch {
                sensorDataService?.isCollecting?.collect { isCollecting ->
                    _collecting.value = isCollecting
                }
            }
            
            viewModelScope.launch {
                sensorDataService?.currentSpeed?.collect { speed ->
                    _currentSpeed.value = speed
                }
            }
            
            viewModelScope.launch {
                sensorDataService?.maxSpeed?.collect { maxSpeed ->
                    _maxSpeed.value = maxSpeed
                }
            }
            
            viewModelScope.launch {
                sensorDataService?.maxGForce?.collect { maxGForce ->
                    _maxGForce.value = maxGForce
                }
            }
            
            viewModelScope.launch {
                sensorDataService?.lastAirTime?.collect { airTime ->
                    _lastAirTime.value = airTime
                }
            }
            
            viewModelScope.launch {
                sensorDataService?.currentLinearAccel?.collect { linearAccel ->
                    _currentLinearAccel.value = linearAccel
                }
            }
            
            viewModelScope.launch {
                sensorDataService?.maxLinearAccel?.collect { maxLinearAccel ->
                    _maxLinearAccel.value = maxLinearAccel
                }
            }
            
            viewModelScope.launch {
                sensorDataService?.saveFileRequestFlow?.collect { saveRequest ->
                    _saveFileRequestFlow.value = saveRequest
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
            sensorDataService = null
            Log.d("MainViewModel", "Service disconnected")
        }
    }

    init {
        bindService()
    }

    fun onLocationPermissionsGranted() {
        _locationPermissionGranted.value = true
    }

    fun getCollecting(): Boolean {
        return _collecting.value
    }

    fun toggleOverallDataCollection() {
        if (bound && sensorDataService != null) {
            if (_collecting.value) {
                sensorDataService?.stopDataCollection()
            } else {
                sensorDataService?.startDataCollection()
            }
        } else {
            Log.w("MainViewModel", "Service not bound, cannot toggle collection")
        }
    }

    private fun bindService() {
        val intent = Intent(getApplication(), SensorDataService::class.java)
        getApplication<Application>().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        
        // Start the service if not already running
        getApplication<Application>().startService(intent)
    }

    fun writeDataToUri(uri: Uri, data: List<TimestampedSensorEvent>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

                    mapper.enable(SerializationFeature.INDENT_OUTPUT)
                    val jsonString: String = mapper.writeValueAsString(data)
                    outputStream.write(jsonString.toByteArray())
                    Log.i("MainViewModel", "All sensor event data saved to URI: $uri")
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
        if (bound) {
            getApplication<Application>().unbindService(connection)
        }
    }

    fun resetMax() {
        sensorDataService?.resetMaxValues()
    }
}