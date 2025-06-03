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

// Hold raw data for processing more later
data class AccelerometerData(val x: Float, val y: Float, val z: Float, val timestamp: Long)
data class GyroscopeData(val x: Float, val y: Float, val z: Float, val timestamp: Long)

// Data class to hold our processed sensor values
data class ProcessedSensorData(
    val totalLinearAcceleration: Float = 0f,
    val gForce: Float = 0f,
    val timestamp: Long = 0L,
    val maxGForce: Float = 0f,
    val maxTotalLinearAcceleration: Float = 0f,
    val accelerometerData: AccelerometerData? = null,
    val gyroscopeData: GyroscopeData? = null
)

class SensorDataProvider(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var linearAccelerationSensor: Sensor? = null
    private var maxGForce = 0f
    private var maxTotalLinearAcceleration = 0f
    private val MOVING_AVERAGE_SIZE = 5 // (3-10 is common)
    private val accelerationReadings = mutableListOf<Float>()

    private val _processedData = MutableStateFlow(ProcessedSensorData())
    val processedData: StateFlow<ProcessedSensorData> = _processedData.asStateFlow()

    private var sensorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    init {
        linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        if (linearAccelerationSensor == null) {
            println("SensorDataProvider: Linear Acceleration Sensor not available.")
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

        sensorJob = scope.launch {
            callbackFlow {
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        event?.let {
                            trySend(it).isSuccess
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    }
                }

                sensorManager.registerListener(
                    listener,
                    linearAccelerationSensor,
                    SensorManager.SENSOR_DELAY_FASTEST
                )

                awaitClose {
                    sensorManager.unregisterListener(listener)
                    println("SensorDataProvider: Listener unregistered.")
                }
            }.onEach { event ->
                processAndEmitSensorData(event)
            }.launchIn(this)
        }
        println("SensorDataProvider: Data collection started.")
    }

    private fun processAndEmitSensorData(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val standardGravity = SensorManager.STANDARD_GRAVITY

            val accelerometerData = AccelerometerData(x, y, z, event.timestamp)
            // TODO need to get gyro data through another sensor
            val gyroscopeData = GyroscopeData(0f, 0f, 0f, event.timestamp)

            val rawTotalLinearAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            accelerationReadings.add(rawTotalLinearAcceleration)
            if (accelerationReadings.size > MOVING_AVERAGE_SIZE) {
                accelerationReadings.removeAt(0)
            }
            val smoothedTotalLinearAcceleration = accelerationReadings.average().toFloat()

            val gForce = smoothedTotalLinearAcceleration / standardGravity

            if (smoothedTotalLinearAcceleration > maxTotalLinearAcceleration) {
                maxTotalLinearAcceleration = smoothedTotalLinearAcceleration
            }
            if (gForce > maxGForce) {
                maxGForce = gForce
            }

            _processedData.value = ProcessedSensorData(
                totalLinearAcceleration = smoothedTotalLinearAcceleration,
                gForce = gForce,
                timestamp = event.timestamp,
                maxGForce = maxGForce,
                maxTotalLinearAcceleration = maxTotalLinearAcceleration,
                accelerometerData = accelerometerData,
                gyroscopeData = gyroscopeData
            )
        }
    }

    fun stopDataCollection() {
        sensorJob?.cancel()
        sensorJob = null
        println("SensorDataProvider: Data collection stopped.")
    }

    fun cleanup() {
        stopDataCollection()
        scope.coroutineContext.cancel()
        println("SensorDataProvider: Cleaned up.")
    }

    fun resetMax() {
        maxGForce = 0f
        maxTotalLinearAcceleration = 0f

    }
}