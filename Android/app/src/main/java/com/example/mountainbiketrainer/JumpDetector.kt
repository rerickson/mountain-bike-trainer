package com.example.mountainbiketrainer

class JumpDetector {
    private var currentJumpState: JumpState = JumpState.ON_GROUND
    private var takeoffTimestamp: Long = 0L

    // TODO tune these
    private val AIRBORNE_G_THRESHOLD = 2.0f
    private val LANDING_G_SPIKE_THRESHOLD = 15.0f
    private val MIN_AIR_TIME_MS = 150

    fun processSensorEvent(data: ProcessedSensorData): Float? { // Returns airtime in seconds
        if(data.accelerometerData == null) return null

        val zForce = data.accelerometerData.z
        val currentTimestamp = data.accelerometerData.timestamp
        var detectedAirTimeSeconds: Float? = null

        when (currentJumpState) {
            JumpState.ON_GROUND -> {
                if (zForce < AIRBORNE_G_THRESHOLD) { // Using a specific threshold for accel
                    println("JumpDetector: Potential Takeoff - Accel Z: $zForce")
                    takeoffTimestamp = currentTimestamp
                    currentJumpState = JumpState.AIRBORNE
                }
            }
            JumpState.AIRBORNE -> {
                // Landing detection
                if (zForce > LANDING_G_SPIKE_THRESHOLD) {
                    val airTimeMillis = currentTimestamp - takeoffTimestamp
                    println("JumpDetector: Potential Landing - Accel Z: $zForce, Air time: $airTimeMillis ms")
                    if (airTimeMillis > MIN_AIR_TIME_MS) {
                        detectedAirTimeSeconds = airTimeMillis / 1000.0f
                        println("JumpDetector: JUMP DETECTED (Accel)! Air time: $detectedAirTimeSeconds s")
                    } else {
                        println("JumpDetector: Too short to be a jump (Accel), likely a bump.")
                    }
                    currentJumpState = JumpState.ON_GROUND
                } else if ((currentTimestamp - takeoffTimestamp) > MAX_EXPECTED_AIR_TIME_MS) {
                    println("JumpDetector: Airborne timeout (Accel), resetting state.")
                    currentJumpState = JumpState.ON_GROUND
                }
            }
        }
        return detectedAirTimeSeconds
    }

    private enum class JumpState { ON_GROUND, AIRBORNE }
    private val MAX_EXPECTED_AIR_TIME_MS = 10000 // e.g., 10 seconds, sanity check
}