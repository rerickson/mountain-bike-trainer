package com.example.mountainbiketrainer

class JumpDetector {
    private var currentJumpState: JumpState = JumpState.ON_GROUND
    private var takeoffTimestamp: Long = 0L

    // TODO tune these
    private val AIRBORNE_G_THRESHOLD = 2.0f
    private val LANDING_G_SPIKE_THRESHOLD = 15.0f

    private val MIN_AIR_TIME_NS = 150 * 1_000_000L
    private val MAX_EXPECTED_AIR_TIME_NS = 10000 * 1_000_000L
    private enum class JumpState { ON_GROUND, AIRBORNE }

    fun processSensorEvent(data: ProcessedSensorData): Float? {
        if(data.accelerometerData == null) return null

        val zForce = data.accelerometerData.z
        val currentTimestamp = data.accelerometerData.timestamp
        var detectedAirTimeSeconds: Float? = null

        when (currentJumpState) {
            JumpState.ON_GROUND -> {
                if (zForce < AIRBORNE_G_THRESHOLD) {
                    println("JumpDetector: Potential Takeoff - Accel Z: $zForce")
                    takeoffTimestamp = currentTimestamp
                    currentJumpState = JumpState.AIRBORNE
                }
            }
            JumpState.AIRBORNE -> {
                if (zForce > LANDING_G_SPIKE_THRESHOLD) {
                    val airTimeNanoSeconds = currentTimestamp - takeoffTimestamp
                    println("JumpDetector: Potential Landing - Accel Z: $zForce, Air time: $airTimeNanoSeconds ns")
                    if (airTimeNanoSeconds > MIN_AIR_TIME_NS) {
                        detectedAirTimeSeconds = airTimeNanoSeconds / 1_000_000_000.0f
                        println("JumpDetector: JUMP DETECTED (Accel)! Air time: $detectedAirTimeSeconds s")
                    } else {
                        println("JumpDetector: Too short to be a jump (Accel), likely a bump.")
                    }
                    currentJumpState = JumpState.ON_GROUND
                } else if ((currentTimestamp - takeoffTimestamp) > MAX_EXPECTED_AIR_TIME_NS) {
                    println("JumpDetector: Airborne timeout (Accel), resetting state. Current timestamp: $currentTimestamp, takeoff timestamp: $takeoffTimestamp difference: ${currentTimestamp - takeoffTimestamp}")
                    currentJumpState = JumpState.ON_GROUND
                }
            }
        }
        return detectedAirTimeSeconds
    }
}