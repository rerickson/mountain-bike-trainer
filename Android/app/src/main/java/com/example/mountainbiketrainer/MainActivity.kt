package com.example.mountainbiketrainer

import TrainerAPIClient
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mountainbiketrainer.ui.theme.MountainBikeTrainerTheme
import java.text.BreakIterator
import androidx.compose.runtime.collectAsState // Important for collecting StateFlow

class MainActivity : ComponentActivity(), SensorEventListener {
    private var linearAccelerationSensor: Sensor? = null
    private var textState by mutableStateOf("Start data collection")
    private var textState2 by mutableStateOf("")
    private var collecting by mutableStateOf(false)
    private lateinit var sensorManager: SensorManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MountainBikeTrainerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        Greeting()
                        StartButton()
                        SaveButton()
                    }
                }
            }
        }
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // Get linear acceleration sensor
        linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        if (linearAccelerationSensor == null) {
            // Fallback or inform user that the sensor is not available
            textState =
                "Linear Acceleration Sensor not available. Using Accelerometer (includes gravity)."
            // Optionally, fall back to using TYPE_ACCELEROMETER and implement filtering
            // For now, let's proceed assuming it might be available, or handle appropriately
            linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (linearAccelerationSensor == null) {
                textState = "No suitable acceleration sensor found."
                // Consider disabling sensor-related functionality
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Register the listener in onResume
        linearAccelerationSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister the listener in onPause to save battery
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!collecting) {
            return
        }

        // Check if it's the linear acceleration sensor (or accelerometer if fallback)
        if (event?.sensor?.type == Sensor.TYPE_LINEAR_ACCELERATION ||
            event?.sensor?.type == Sensor.TYPE_ACCELEROMETER
        ) { // Added accelerometer for fallback
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2] // Z-axis might still be useful for total movement magnitude

            // Calculate horizontal acceleration (movement in the phone's XY plane)
            val horizontalAcceleration = Math.sqrt((x * x + y * y).toDouble()).toFloat()

            // Or, calculate the magnitude of the total linear acceleration vector
            val totalLinearAcceleration = Math.sqrt((x * x + y * y + z*z).toDouble()).toFloat()

            //TODO add smoothing with low pass filter
            //TODO add Kalman filter

            textState = "Horizontal Linear Accel: $horizontalAcceleration m/s^2"

            textState2 = "Total Linear Accel: $totalLinearAcceleration m/s^2"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Can be used to handle changes in sensor accuracy if needed
    }

    @Composable
    fun Greeting(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier, // Apply the passed-in modifier to the Column
            verticalArrangement = Arrangement.spacedBy(8.dp) // Adds 8.dp space between each child
        ) {
            Text(text = textState)    // First Text element
            Text(text = textState2)   // Second Text element, now directly in the Column
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        MountainBikeTrainerTheme {
            Greeting()
        }
    }

    @Composable
    fun StartButton() {
        var buttonText by remember { mutableStateOf("Start") }
        Button(
            onClick = {
                collecting = !collecting
                buttonText = if (collecting) "Stop" else "Start"
            },
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 8.dp,
                top = 70.dp
            ) // Adjusted padding
        ) {
            Text(text = buttonText)
        }
    }

    @Composable
    fun SaveButton() {
        Button(
            onClick = {
                val client = TrainerAPIClient() // Corrected instantiation
                client.saveData()
            },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp) // Adjusted padding
        ) {
            Text(text = "Save")
        }
    }
}
