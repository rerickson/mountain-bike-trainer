package com.example.mountainbiketrainer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                // Precise location access granted.
                // Start your location-dependent operations (e.g., tell ViewModel to start collecting)
                // mainViewModel.onPermissionsGranted() // Example call
                showLocationAccessState.value = "Fine location granted"
                // Now you can safely start collecting from your LocationProvider
            } else if (coarseLocationGranted) {
                // Only approximate location access granted.
                // Adjust your app's behavior accordingly.
                showLocationAccessState.value = "Coarse location granted"
            } else {
                // No location access granted.
                // Inform the user that the feature is unavailable because
                // the feature requires a permission that the user has denied.
                // You might want to show a dialog explaining why the permission is needed
                // and how they can grant it in settings.
                showLocationAccessState.value = "Location permission denied"
                showPermissionRationaleDialog.value = true
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setContent {
            MountainBikeTrainerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    LocationPermissionScreenContent()
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

    @Composable
    fun SpeedDisplay(viewModel: MainViewModel) {
        val speedData by viewModel.currentSpeed.collectAsState()
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Speed: ${"%.2f".format(speedData?.speedMph)} mph")
        }
    }

    @Composable
    fun SensorDisplay(viewModel: MainViewModel) {
        val sensorData by viewModel.processedSensorData.collectAsState()
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Total Linear Accel: ${"%.2f".format(sensorData.totalLinearAcceleration)} m/s^2")
            Text("G-Force: ${"%.2f".format(sensorData.gForce)} Gs")
            Text("Timestamp: ${sensorData.timestamp}")
            Text("Max G-Force: ${"%.2f".format(sensorData.maxGForce)} Gs")
            Text("Max Total Linear Accel: ${"%.2f".format(sensorData.maxTotalLinearAcceleration)} m/s^2")

            Button(onClick = { viewModel.toggleOverallDataCollection() }) {
                val buttonText = if (!viewModel.getCollecting()) "Start" else "Stop"
                Text(buttonText)
            }

            Button(onClick = { viewModel.resetMax() }) {
                Text("Reset")
            }
        }
    }

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
    fun LocationPermissionScreenContent() {
        checkAndRequestLocationPermissions()
        if (showLocationAccessState.value.contains("granted")) { // Or a better state check
            MountainBikeTrainerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    Column (
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ){
                        SpeedDisplay(viewModel = viewModel)
                        SensorDisplay(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
