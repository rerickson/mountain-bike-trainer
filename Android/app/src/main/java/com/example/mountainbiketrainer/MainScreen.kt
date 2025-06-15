package com.example.mountainbiketrainer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun MainScreen(
    navController: NavController,
    mainViewModel: MainViewModel,
    locationPermissionStatus: LocationPermissionStatus,
    onRequestPermissions: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("MTB Trainer", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
//        Button(onClick = {
//            navController.navigate("sessionFiles")
//        }) {
//            Text("View Recorded Sessions")
//        }

        // Content based on permission status
        when (locationPermissionStatus) {
            LocationPermissionStatus.CHECKING -> {
                CircularProgressIndicator()
                Text("Checking permissions...")
            }
            LocationPermissionStatus.GRANTED_FINE, LocationPermissionStatus.GRANTED_COARSE -> {
                // Can enable this again if we run into location issues
//                Text(if (locationPermissionStatus == LocationPermissionStatus.GRANTED_FINE) "Fine location granted" else "Coarse location granted", color = MaterialTheme.colorScheme.primary)
                LocationDataContent(mainViewModel = mainViewModel) // Extracted the actual data display
            }
            LocationPermissionStatus.DENIED -> {
                Text("Location permission denied.", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRequestPermissions) {
                    Text("Grant Permissions")
                }
            }
            LocationPermissionStatus.DENIED_RATIONALE -> {
                Text("Location permission is needed for core functionality.", color = MaterialTheme.colorScheme.error)
                Text("Please grant the permission to continue.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRequestPermissions) {
                    Text("Grant Permissions")
                }
            }
        }
    }
}

@Composable
fun LocationDataContent(mainViewModel: MainViewModel) {
    val airTimeValue by mainViewModel.lastAirTime.collectAsState()

    Column (
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { mainViewModel.toggleOverallDataCollection() }) {
                val buttonText = if (!mainViewModel.getCollecting()) "Start Recording" else "Stop Recording"
                Text(buttonText)
            }

            Button(onClick = { mainViewModel.resetMax() }) {
                Text("Reset Max Values")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Data Displays - these should ideally be in their own file or section
        val speedData by mainViewModel.currentSpeed.collectAsState()
        SpeedDisplay(speedData?.speedMps)

        val maxSpeedData by mainViewModel.maxSpeed.collectAsState()
        MaxSpeedDisplay(maxSpeedData?.speedMps)

        Spacer(modifier = Modifier.height(8.dp))
        GForceDisplay(mainViewModel.maxGForce.collectAsState().value)
        AirTimeDisplay(airTimeValue)

        Spacer(modifier = Modifier.height(16.dp))
        Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(8.dp))

        SessionStatsDisplay(mainViewModel)
    }
}

@Composable
fun SpeedDisplay(speedMph: Float?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
fun MaxSpeedDisplay(speedMph: Float?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "MAX SPEED",
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
fun GForceDisplay(gForceData: Float?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "MAX G-FORCE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (gForceData != null) "%.2f".format(gForceData) + "g" else "-.--g",
            fontSize = 48.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun AirTimeDisplay(airTime: Float?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "LAST AIR TIME"
        )
        Text(
            text = if (airTime != null) String.format("%.2f s", airTime) else "-.--s",
            fontSize = 48.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun SessionStatsDisplay(viewModel: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val maxLinearAccel = viewModel.maxLinearAccel.collectAsState().value
            val currentLinearAccel = viewModel.currentLinearAccel.collectAsState().value

            StatItem("Max Linear Accel", if (maxLinearAccel != null) "%.1f m/s^2".format(maxLinearAccel.z) else "--")
            StatItem("Current Linear Accel", if (currentLinearAccel != null) "%.1f m/s^2".format(currentLinearAccel.z) else "--")
            StatItem("Timestamp", (currentLinearAccel?.timestamp ?: "--").toString())

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