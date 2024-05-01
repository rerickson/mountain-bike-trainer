package com.example.mountainbiketrainer

import TrainerAPIClient
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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


class MainActivity : ComponentActivity(), SensorEventListener {
    private var accelerometer: Sensor? = null
    private var textState by mutableStateOf("Start data collection")
    private var collecting by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MountainBikeTrainerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column() {
                        Greeting()
                    }
                    Column() {
                        StartButton()
                        SaveButton()
                    }
                }
            }
        }
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // Get accelerometer sensor
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(!collecting){
            return
        }

        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val horizontalAcceleration = Math.sqrt((x * x + y * y).toDouble()).toFloat()

            updateText("horizontal accel: " + horizontalAcceleration)
        }
    }

    private fun updateText(newText: String) {
        textState = newText
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    @Composable
    fun Greeting(modifier: Modifier = Modifier) {
//    var accelerationState by remember { mutableStateOf(0.0f) }
        Text(
            text = textState,
            modifier = modifier
        )

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
                buttonText = if(collecting) "Stop" else "Start"
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = buttonText)
        }
    }

    @Composable
    fun SaveButton(){
        Button(
            onClick = {
                var client = TrainerAPIClient();
                client.saveData();

            },
            modifier = Modifier.padding(16.dp)
        ){
            Text(text = "Save")
        }
    }
}
