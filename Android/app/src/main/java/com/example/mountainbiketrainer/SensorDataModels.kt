package com.example.mountainbiketrainer

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes

@JsonSubTypes(
    JsonSubTypes.Type(value = AccelerometerEvent::class, name = "AccelerometerEvent"),
    JsonSubTypes.Type(value = GyroscopeEvent::class, name = "GyroscopeEvent"),
    JsonSubTypes.Type(value = LinearAccelEvent::class, name = "LinearAccelerationEvent"),
    JsonSubTypes.Type(value = MagneticFieldEvent::class, name = "MagneticFieldEvent"),
    JsonSubTypes.Type(value = PressureEvent::class, name = "BarometerEvent"),
    JsonSubTypes.Type(value = GravityEvent::class, name = "GravityEvent"),
    JsonSubTypes.Type(value = RotationVectorEvent::class, name = "RotationVectorEvent"),
    JsonSubTypes.Type(value = GPSSpeedEvent::class, name = "GPSSpeedEvent"),
    JsonSubTypes.Type(value = GPSLocationEvent::class, name = "GPSLocationEvent")
)
interface TimestampedSensorEvent {
    val timestamp: Long
    // 'event_type' will be added by Jackson due to @JsonTypeInfo
}

data class AccelerometerEvent(
    override val timestamp: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val eventType: String = "AccelerometerEvent"
) : TimestampedSensorEvent

data class GyroscopeEvent(
    override val timestamp: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val eventType: String = "GyroscopeEvent"
) : TimestampedSensorEvent

data class LinearAccelEvent(
    override val timestamp: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val eventType: String = "LinearAccelerationEvent"
) : TimestampedSensorEvent

data class MagneticFieldEvent(
    override val timestamp: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val eventType: String = "MagneticFieldEvent"
) : TimestampedSensorEvent


data class PressureEvent(
    override val timestamp: Long,
    val pressure: Float,
    val altitude: Float?, // Optional altitude
    val eventType: String = "BarometerEvent"
) : TimestampedSensorEvent

data class GravityEvent(
    override val timestamp: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val eventType: String = "GravityEvent"
) : TimestampedSensorEvent

data class RotationVectorEvent(
    override val timestamp: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float?, // Scalar component, often present, make sure it's it.values[3]
    @get:JsonProperty("heading_accuracy") // Example of customizing JSON field name
    val headingAccuracy: Int?, // SensorEvent.accuracy, not always available or a heading
    val eventType: String = "RotationVectorEvent"
) : TimestampedSensorEvent

data class GPSSpeedEvent(
    override val timestamp: Long,
    @get:JsonProperty("speed_mps")
    val speedMps: Float,
    @get:JsonProperty("speed_accuracy_mps")
    val accuracyMps: Float?,
    val eventType: String = "GPSSpeedEvent" 
) : TimestampedSensorEvent

data class GPSLocationEvent(
    override val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?, // This is GPS altitude, different from barometric
    @get:JsonProperty("horizontal_accuracy_m")
    val accuracyHorizontal: Float?,
    val eventType: String = "GPSLocationEvent"
) : TimestampedSensorEvent