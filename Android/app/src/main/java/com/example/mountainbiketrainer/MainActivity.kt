package com.example.mountainbiketrainer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mountainbiketrainer.ui.theme.MountainBikeTrainerTheme

class MainActivity : ComponentActivity() {

    private lateinit var mainViewModel: MainViewModel
    private lateinit var sessionViewModel: SessionViewModel

    // --- Permission Handling Logic ---
    private lateinit var requestLocationPermissionLauncher: ActivityResultLauncher<Array<String>>
    private val _locationPermissionState = mutableStateOf(LocationPermissionStatus.CHECKING)
    val locationPermissionState: State<LocationPermissionStatus> = _locationPermissionState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        sessionViewModel = ViewModelProvider(this)[SessionViewModel::class.java]

        requestLocationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

                if (fineLocationGranted) {
                    _locationPermissionState.value = LocationPermissionStatus.GRANTED_FINE
                    mainViewModel.onLocationPermissionsGranted() // Notify ViewModel
                } else if (coarseLocationGranted) {
                    _locationPermissionState.value = LocationPermissionStatus.GRANTED_COARSE
                    mainViewModel.onLocationPermissionsGranted() // Notify ViewModel
                } else {
                    _locationPermissionState.value = LocationPermissionStatus.DENIED
                }
            }

        checkAndRequestLocationPermissions()

        setContent {
            MountainBikeTrainerTheme(dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation(
                        mainViewModel = mainViewModel,
                        sessionViewModel = sessionViewModel,
                        locationPermissionStatus = locationPermissionState.value, // Pass current status
                        onRequestPermissions = { checkAndRequestLocationPermissions() } // Pass request function
                    )
                }
            }
        }
    }

    fun checkAndRequestLocationPermissions() {
        _locationPermissionState.value = LocationPermissionStatus.CHECKING
        val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION

        val fineLocationGranted = ContextCompat.checkSelfPermission(this, fineLocationPermission) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(this, coarseLocationPermission) == PackageManager.PERMISSION_GRANTED

        when {
            fineLocationGranted -> {
                _locationPermissionState.value = LocationPermissionStatus.GRANTED_FINE
                mainViewModel.onLocationPermissionsGranted()
            }
            coarseLocationGranted -> {
                _locationPermissionState.value = LocationPermissionStatus.GRANTED_COARSE
                mainViewModel.onLocationPermissionsGranted()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                    shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                // This is where you'd typically show a custom rationale dialog
                // For now, we'll mark as denied and let MainScreen handle the UI
                _locationPermissionState.value = LocationPermissionStatus.DENIED_RATIONALE
                // _showPermissionRationaleDialog.value = true
            }
            else -> {
                // Request permissions
                requestLocationPermissionLauncher.launch(
                    arrayOf(fineLocationPermission, coarseLocationPermission)
                )
            }
        }
    }
}

// Enum to represent permission status more clearly
enum class LocationPermissionStatus {
    CHECKING,
    GRANTED_FINE,
    GRANTED_COARSE,
    DENIED,
    DENIED_RATIONALE // When rationale should be shown
}

// Your AppNavigation needs to accept and pass these down
@Composable
fun AppNavigation(
    mainViewModel: MainViewModel,
    sessionViewModel: SessionViewModel,
    locationPermissionStatus: LocationPermissionStatus,
    onRequestPermissions: () -> Unit
) {
    val navController: NavHostController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "mainScreen"
    ) {
        composable(route = "mainScreen") {
            MainScreen(
                navController = navController,
                mainViewModel = mainViewModel,
                locationPermissionStatus = locationPermissionStatus,
                onRequestPermissions = onRequestPermissions
            )
        }
        composable(route = "sessionFiles") {
            SessionScreen(navController = navController, sessionViewModel = sessionViewModel)
        }
    }
}