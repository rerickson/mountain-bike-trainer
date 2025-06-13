package com.example.mountainbiketrainer

class SensorDataModels {
    // Base class or interface (optional, but can be useful)
    interface TimestampedSensorEvent {
        val type: String
        val timestamp: Long // Use Long for nanoseconds or milliseconds consistently
    }

    data class AccelerometerEvent(
        override val timestamp: Long, // SensorEvent.timestamp (nanoseconds)
        val x: Float,
        val y: Float,
        val z: Float,
        override val type: String = "accelerometer"
    ) : TimestampedSensorEvent

    data class GyroscopeEvent(
        override val timestamp: Long, // SensorEvent.timestamp (nanoseconds)
        val x: Float,
        val y: Float,
        val z: Float,
        override val type: String = "gyroscope"
    ) : TimestampedSensorEvent

    data class GPSSpeedEvent(
        override val timestamp: Long, // Location.getTime() (milliseconds)
        val speedMps: Float, // Speed in meters per second
        val accuracyMps: Float?, // Optional: Location.getSpeedAccuracyMetersPerSecond()
        override val type: String = "gps_speed"
    ) : TimestampedSensorEvent

    data class GPSLocationEvent(
        override val timestamp: Long, // Location.getTime()
        val latitude: Double,
        val longitude: Double,
        val altitude: Double?,
        val accuracyHorizontal: Float?,
        override val type: String = "gps_location"
    ) : TimestampedSensorEvent

}