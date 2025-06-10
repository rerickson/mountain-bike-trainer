package com.example.mountainbiketrainer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mountainbiketrainer.ui.theme.MountainBikeTrainerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var mainViewModel: MainViewModel
    private lateinit var sessionViewModel: SessionViewModel

    // --- Permission Handling Logic ---
    private lateinit var requestLocationPermissionLauncher: ActivityResultLauncher<Array<String>>
    private val _locationPermissionState = mutableStateOf(LocationPermissionStatus.CHECKING)
    val locationPermissionState: State<LocationPermissionStatus> = _locationPermissionState

    private lateinit var createFileLauncher: ActivityResultLauncher<Intent>
    private var pendingSaveRequest: SaveFileRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        sessionViewModel = ViewModelProvider(this)[SessionViewModel::class.java]

        createFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    // URI received, now write the data using the ViewModel
                    pendingSaveRequest?.let { request ->
                        mainViewModel.writeDataToUri(uri, request.dataToSave)
                        pendingSaveRequest = null // Clear the pending request
                    }
                }
            } else {
                // Handle cancellation or failure
                android.util.Log.w("MainActivity", "File creation was cancelled or failed.")
                pendingSaveRequest = null
            }
        }
        // Observe requests from ViewModel to save a file
        lifecycleScope.launch { // Use lifecycleScope for long-running UI-related coroutines
            mainViewModel.saveFileRequestFlow.collect { request ->
                pendingSaveRequest = request // Store the data temporarily
                createJsonFile(request.suggestedName)
            }
        }

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

    private fun createJsonFile(suggestedName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json" // MIME type for JSON
            putExtra(Intent.EXTRA_TITLE, suggestedName)

             val documentsDirUri = DocumentsContract.buildDocumentUri(
                 "com.android.externalstorage.documents",
                 "primary:Documents"
             )
             putExtra(DocumentsContract.EXTRA_INITIAL_URI, documentsDirUri)
        }
        createFileLauncher.launch(intent)
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
                _locationPermissionState.value = LocationPermissionStatus.DENIED_RATIONALE
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