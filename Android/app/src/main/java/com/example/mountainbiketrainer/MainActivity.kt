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
        if (!coarseLocationGranted) {
            permissionsToRequest.add(coarseLocationPermission)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestLocationPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            showLocationAccessState.value = "granted."
            viewModel.onLocationPermissionsGranted()
        }
    }


    @Composable
    fun LocationPermissionScreenContent(viewModel: MainViewModel) {
        checkAndRequestLocationPermissions()
        if (showLocationAccessState.value.contains("granted")) {
            val sensorData by viewModel.processedSensorData.collectAsState()

            Column (
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ){
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // TODO remove this
                    Text(showLocationAccessState.value)

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
                    GForceDisplay(sensorData)
                }

//                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                SessionStatsDisplay(sensorData)
                Spacer(modifier = Modifier.height(24.dp))
            }
        } else {
            Text("Permissions required")
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
                fontSize = 72.sp,
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
    fun GForceDisplay(gForceData: ProcessedSensorData?) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "G-FORCE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (gForceData != null) "%.2f".format(gForceData.maxGForce) + "g" else "-.--g",
                fontSize = 48.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
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
//                StatItem("Max G-Force", if (stats?.maxGForce != null) "%.2fg".format(stats.maxGForce) else "--")
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
