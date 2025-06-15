package com.example.mountainbiketrainer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SensorDataProvider(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) // Using raw accelerometer
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val linearAccel: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val barometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val pressure: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    private val gravity: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val rotationVector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val geomagneticField: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)

    // You'll emit a Flow of the base type or Any, and then filter in the ViewModel
    fun sensorEventsFlow(): Flow<TimestampedSensorEvent> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val sensorEventData: TimestampedSensorEvent? = when (it.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> AccelerometerEvent(
                            timestamp = it.timestamp,
                            x = it.values[0],
                            y = it.values[1],
                            z = it.values[2]
                        )
                        Sensor.TYPE_GYROSCOPE -> GyroscopeEvent(
                            timestamp = it.timestamp,
                            x = it.values[0],
                            y = it.values[1],
                            z = it.values[2]
                        )
                        Sensor.TYPE_LINEAR_ACCELERATION -> LinearAccelEvent(
                            timestamp = it.timestamp,
                            x = it.values[0],
                            y = it.values[1],
                            z = it.values[2]
                        )
                        Sensor.TYPE_MAGNETIC_FIELD -> MagneticFieldEvent(
                            timestamp = it.timestamp,
                            x = it.values[0],
                            y = it.values[1],
                            z = it.values[2]
                        )
                        Sensor.TYPE_PRESSURE -> {
                            val currentPressure = it.values[0]
                            // Basic altitude calculation
                            val altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, currentPressure)
                            PressureEvent(
                                timestamp = it.timestamp,
                                pressure = currentPressure,
                                altitude = altitude
                            )
                        }
                        Sensor.TYPE_GRAVITY -> GravityEvent(
                            timestamp = it.timestamp,
                            x = it.values[0],
                            y = it.values[1],
                            z = it.values[2]
                        )
                        Sensor.TYPE_ROTATION_VECTOR -> {
                            // Rotation vector can have 3, 4, or 5 values.
                            // values[0]: x*sin(θ/2)
                            // values[1]: y*sin(θ/2)
                            // values[2]: z*sin(θ/2)
                            // values[3]: cos(θ/2) (scalar component, optional, only if array length >= 4)
                            // values[4]: estimated heading accuracy (optional, only if array length >= 5)
                            val wValue = if (it.values.size >= 4) it.values[3] else null
                            val accuracyValue = if (it.values.size >= 5) it.values[4].toInt() else it.accuracy // Fallback to event.accuracy if specific value not present
                            RotationVectorEvent(
                                timestamp = it.timestamp,
                                x = it.values[0],
                                y = it.values[1],
                                z = it.values[2],
                                w = wValue,
                                headingAccuracy = accuracyValue
                            )
                        }
                        else -> null
                    }
                    sensorEventData?.let { trySend(it).isSuccess }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* TODO */ }
        }

        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
        }
        if (gyroscope != null) {
            sensorManager.registerListener(listener, gyroscope, SensorManager.SENSOR_DELAY_FASTEST)
        }
        if(linearAccel != null) {
            sensorManager.registerListener(listener, linearAccel, SensorManager.SENSOR_DELAY_FASTEST)
        }
        if(barometer != null) {
            sensorManager.registerListener(listener, barometer, SensorManager.SENSOR_DELAY_FASTEST)
        }
        if(pressure != null) {
            sensorManager.registerListener(listener, pressure, SensorManager.SENSOR_DELAY_FASTEST)
        }
        if(gravity != null) {
            sensorManager.registerListener(listener, gravity, SensorManager.SENSOR_DELAY_FASTEST)
        }
        if(rotationVector != null) {
            sensorManager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_FASTEST)
        }
        if(geomagneticField != null) {
            sensorManager.registerListener(listener, geomagneticField, SensorManager.SENSOR_DELAY_FASTEST)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}