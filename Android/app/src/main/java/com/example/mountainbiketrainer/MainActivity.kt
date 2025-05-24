package com.example.mountainbiketrainer

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.mountainbiketrainer.ui.theme.MountainBikeTrainerTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        setContent {
            MountainBikeTrainerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SensorDisplay(viewModel = viewModel)
                }
            }
        }
    }

    @Composable
    fun SensorDisplay(viewModel: MainViewModel) { // Pass ViewModel or get it via hiltViewModel()
        // Collect the StateFlow as State. Compose will automatically recompose
        // when this state changes.
        val sensorData by viewModel.processedSensorData.collectAsState()
        var collecting = false
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Total Linear Accel: ${"%.2f".format(sensorData.totalLinearAcceleration)} m/s^2")
            Text("G-Force: ${"%.2f".format(sensorData.gForce)} Gs")
            Text("Timestamp: ${sensorData.timestamp}")
            Text("Max G-Force: ${"%.2f".format(sensorData.maxGForce)} Gs")
            Text("Max Total Linear Accel: ${"%.2f".format(sensorData.maxTotalLinearAcceleration)} m/s^2")

            Button(onClick = { viewModel.toggleDataCollection() }) {
                // You might want another StateFlow in ViewModel for the button text ("Start"/"Stop")
                val buttonText = if (!collecting) "Start" else "Stop" // Basic logic
                collecting = !collecting
                Text(buttonText)
            }

            Button(onClick = { viewModel.resetMax() }) {

                Text("Reset")
            }
        }
    }
}
