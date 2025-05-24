package com.example.mountainbiketrainer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.sqrt

// Data class to hold our processed sensor values
data class ProcessedSensorData(
    val totalLinearAcceleration: Float = 0f,
    val gForce: Float = 0f,
    val timestamp: Long = 0L, // Optional: include timestamp of the event
    val maxGForce: Float = 0f,
    val maxTotalLinearAcceleration: Float = 0f
)

class SensorDataProvider(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var linearAccelerationSensor: Sensor? = null
    private var maxGForce = 0f
    private var maxTotalLinearAcceleration = 0f

    // Use MutableStateFlow to hold the latest processed data
    // Initialize with default/empty data
    private val _processedData = MutableStateFlow(ProcessedSensorData())
    val processedData: StateFlow<ProcessedSensorData> = _processedData.asStateFlow()

    private var sensorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + Job()) // Use a dedicated scope

    init {
        linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        // You might want to add fallback to TYPE_ACCELEROMETER here as well
        if (linearAccelerationSensor == null) {
            println("SensorDataProvider: Linear Acceleration Sensor not available.")
            // Potentially emit an error state or use a fallback
        }
    }

    fun startDataCollection() {
        if (linearAccelerationSensor == null) {
            println("SensorDataProvider: Cannot start collection, sensor not available.")
            return
        }
        if (sensorJob?.isActive == true) {
            println("SensorDataProvider: Collection already active.")
            return
        }

        // Launch a new coroutine for sensor data collection
        sensorJob = scope.launch {
            // callbackFlow is a good way to bridge callback-based APIs like SensorEventListener
            // with Kotlin Flows.
            callbackFlow {
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        event?.let {
                            // Try to offer the event to the flow.
                            // If the channel is full (backpressure), it might suspend or drop.
                            // For sensor data, 'trySend' is often fine as we only care about the latest.
                            trySend(it).isSuccess
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                        // Handle accuracy changes if needed
                    }
                }

                sensorManager.registerListener(
                    listener,
                    linearAccelerationSensor,
                    SensorManager.SENSOR_DELAY_FASTEST // Collect data as fast as possible
                )

                // Unregister listener when the flow is cancelled (coroutine is stopped)
                awaitClose {
                    sensorManager.unregisterListener(listener)
                    println("SensorDataProvider: Listener unregistered.")
                }
            }.onEach { event -> // Process each event received from the flow
                processAndEmitSensorData(event)
            }.launchIn(this) // Collect the flow within this coroutine
        }
        println("SensorDataProvider: Data collection started.")
    }

    private fun processAndEmitSensorData(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val standardGravity = SensorManager.STANDARD_GRAVITY

            val totalLinearAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val gForce = totalLinearAcceleration / standardGravity

            // TODO - Add in smoothing with low pass filter

            // TODO - Add in Kalman filter

            if(gForce > maxGForce){
                maxGForce = gForce
            }

            if(totalLinearAcceleration > maxTotalLinearAcceleration){
                maxTotalLinearAcceleration = totalLinearAcceleration
            }

            _processedData.value = ProcessedSensorData(
                totalLinearAcceleration = totalLinearAcceleration,
                gForce = gForce,
                timestamp = event.timestamp,
                maxGForce = maxGForce,
                maxTotalLinearAcceleration = maxTotalLinearAcceleration
            )
        }
    }

    fun stopDataCollection() {
        sensorJob?.cancel()
        sensorJob = null
        // Optionally reset the data when stopped
        // _processedData.value = ProcessedSensorData()
        println("SensorDataProvider: Data collection stopped.")
    }

    // Call this when the component holding SensorDataProvider is destroyed (e.g., ViewModel's onCleared)
    fun cleanup() {
        stopDataCollection()
        scope.coroutineContext.cancel() // Cancel all coroutines in this scope
        println("SensorDataProvider: Cleaned up.")
    }

    fun resetMax() {
        maxGForce = 0f
        maxTotalLinearAcceleration = 0f

    }
}