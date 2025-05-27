package com.example.mountainbiketrainer

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.mountainbiketrainer.ui.theme.MountainBikeTrainerTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel
    private val showLocationAccessState = mutableStateOf("Checking permissions...")
    private val showPermissionRationaleDialog = mutableStateOf(false)

    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineLocationGranted) {
                showLocationAccessState.value = "Fine location granted"
            } else if (coarseLocationGranted) {
                showLocationAccessState.value = "Coarse location granted"
            } else {
                showLocationAccessState.value = "Location permission denied"
                showPermissionRationaleDialog.value = true
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setContent {
            MountainBikeTrainerTheme (dynamicColor = false){
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    LocationPermissionScreenContent(viewModel)
                }
            }
        }
        checkInitialPermissionStatus()
    }

    private fun checkInitialPermissionStatus() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted || coarseLocationGranted) {
            showLocationAccessState.value = "Location permissions already granted."
            viewModel.onLocationPermissionsGranted()
        } else {
            showLocationAccessState.value = "Location permissions not granted yet."
            viewModel.onLocationPermissionsDenied() // Ensure initial state is correct
        }
    }

//    @Composable
//    fun SpeedDisplay(viewModel: MainViewModel) {
//        val speedData by viewModel.currentSpeed.collectAsState()
//        Column(
//            modifier = Modifier.padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            Text("Speed: ${"%.2f".format(speedData?.speedMph)} mph")
//        }
//    }

//    @Composable
//    fun SensorDisplay(viewModel: MainViewModel) {
//        val sensorData by viewModel.processedSensorData.collectAsState()
//        Column(
//            modifier = Modifier.padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//
//        ) {
//            Text("Total Linear Accel: ${"%.2f".format(sensorData.totalLinearAcceleration)} m/s^2")
//            Text("G-Force: ${"%.2f".format(sensorData.gForce)} Gs")
//            Text("Timestamp: ${sensorData.timestamp}")
//            Text("Max G-Force: ${"%.2f".format(sensorData.maxGForce)} Gs")
//            Text("Max Total Linear Accel: ${"%.2f".format(sensorData.maxTotalLinearAcceleration)} m/s^2")
//
//        }
//    }

    private fun checkAndRequestLocationPermissions() {
        val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION

        val fineLocationGranted = ContextCompat.checkSelfPermission(this, fineLocationPermission) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(this, coarseLocationPermission) == PackageManager.PERMISSION_GRANTED

        val permissionsToRequest = mutableListOf<String>()
        if (!fineLocationGranted) {
            permissionsToRequest.add(fineLocationPermission)
        }
        if (!coarseLocationGranted) { // Also request coarse if fine is not enough or you want to offer it
            permissionsToRequest.add(coarseLocationPermission)
        }

        if (permissionsToRequest.isNotEmpty()) {
            // Explain why you need the permission (rationale)
            // if (shouldShowRequestPermissionRationale(fineLocationPermission) || shouldShowRequestPermissionRationale(coarseLocationPermission)) {
            //     // Show a dialog explaining why you need the permission.
            //     // After the user sees the explanation, try requesting again.
            //     showLocationAccessState.value = "Please grant location permission for the app to function."
            //     showPermissionRationaleDialog.value = true // Trigger rationale dialog
            // } else {
            // No explanation needed; request the permission
            requestLocationPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            // }
        } else {
            // Permissions are already granted
            showLocationAccessState.value = "Location permissions already granted."
            // Proceed with location-dependent tasks
            //mainViewModel.onPermissionsGranted() // Example
        }
    }


    @Composable
    fun LocationPermissionScreenContent(viewModel: MainViewModel) {
        checkAndRequestLocationPermissions()
        if (showLocationAccessState.value.contains("granted")) { // Or a better state check
            val sensorData by viewModel.processedSensorData.collectAsState()
            MountainBikeTrainerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column (
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ){
                        Column(
                            modifier = Modifier.weight(1f), // Takes up available space, pushing stats to bottom
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {

                            Button(onClick = { viewModel.toggleOverallDataCollection() }) {
                                val buttonText = if (!viewModel.getCollecting()) "Start" else "Stop"
                                Text(buttonText)
                            }

                            Button(onClick = { viewModel.resetMax() }) {
                                Text("Reset")
                            }

                            val speedData by viewModel.currentSpeed.collectAsState()
                            SpeedDisplay(speedData?.speedMph)
                            Spacer(modifier = Modifier.height(24.dp))
                            GForceDisplay(sensorData) // Pass your G-force data
                        }

                        // Divider
//                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Secondary Stats Area
                        SessionStatsDisplay(sensorData) // Pass your session stats
                    }
                }
            }
        }
    }

    @Composable
    fun SpeedDisplay(speedMph: Float?) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text(
                text = "SPEED",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (speedMph != null) "%.1f".format(speedMph) else "--.-",
                fontSize = 72.sp, // Large font for speed
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "MPH",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    fun GForceDisplay(gForceData: ProcessedSensorData?) { // Assuming GForceData class

//        Text("Total Linear Accel: ${"%.2f".format(sensorData.totalLinearAcceleration)} m/s^2")
//        Text("G-Force: ${"%.2f".format(sensorData.gForce)} Gs")
//        Text("Timestamp: ${sensorData.timestamp}")
//        Text("Max G-Force: ${"%.2f".format(sensorData.maxGForce)} Gs")
//        Text("Max Total Linear Accel: ${"%.2f".format(sensorData.maxTotalLinearAcceleration)} m/s^2")
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "G-FORCE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // You can choose how to display G-Force. Here's one option:
            // Option 1: Displaying Max Lateral G as primary, or individual axes
            Text(
                // Example: Displaying the more relevant lateral G-force or a composite value
                text = if (gForceData != null) "%.2f".format(gForceData.maxGForce) + "g" else "-.--g",
                fontSize = 48.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )

            // Option 2: Individual Axes (if important) - can be smaller
            // Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            //     GForceAxis(label = "X", value = gForceData?.x)
            //     GForceAxis(label = "Y", value = gForceData?.y)
            //     GForceAxis(label = "Z", value = gForceData?.z)
            // }
        }
    }

    @Composable
    fun GForceAxis(label: String, value: Float?) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (value != null) "%.2f".format(value) else "-.--",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }


    @Composable
    fun SessionStatsDisplay(stats: ProcessedSensorData?) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem("Max Linear Accel", if (stats?.maxTotalLinearAcceleration != null) "%.1f m/s^2".format(stats.maxTotalLinearAcceleration) else "--")
                StatItem("Current Linear Accel", if (stats?.totalLinearAcceleration != null) "%.1f m/s^2".format(stats.totalLinearAcceleration) else "--")
                StatItem("Timestamp", (stats?.timestamp ?: "--").toString())

            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
//                StatItem("Distance", if (stats?.distanceMiles != null) "%.1f mi".format(stats.distanceMiles) else "--")
                StatItem("Max G-Force", if (stats?.maxGForce != null) "%.2fg".format(stats.maxGForce) else "--")
            }
            //StatItem("Time", stats?.elapsedTimeFormatted ?: "--:--:--")
        }
    }

    @Composable
    fun StatItem(label: String, value: String) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
