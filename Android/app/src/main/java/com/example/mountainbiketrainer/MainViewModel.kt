package com.example.mountainbiketrainer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val sensorDataProvider = SensorDataProvider(application.applicationContext)

    // Expose the StateFlow from the provider
    val processedSensorData: StateFlow<ProcessedSensorData> = sensorDataProvider.processedData

    private var isCollecting = false // Internal state to track collection

    fun toggleDataCollection() {
        isCollecting = !isCollecting
        if (isCollecting) {
            sensorDataProvider.startDataCollection()
        } else {
            sensorDataProvider.stopDataCollection()
        }
    }

    // It's important to clean up the SensorDataProvider when the ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        sensorDataProvider.cleanup()
    }

    fun resetMax() {
        sensorDataProvider.resetMax()
    }
}