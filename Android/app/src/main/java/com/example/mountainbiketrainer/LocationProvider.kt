import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

data class SpeedData(val speedMph: Float)

class LocationProvider(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null

    private val _locationUpdatesFlow = MutableStateFlow<Flow<SpeedData>>(emptyFlow())
    val locationUpdates: Flow<SpeedData> = _locationUpdatesFlow.asStateFlow().value

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        MIN_TIME_BW_UPDATES_MS
    )
        .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
        .setMinUpdateDistanceMeters(MIN_DISTANCE_CHANGE_FOR_UPDATES_METERS)
        // .setWaitForAccurateLocation(true) // TODO Try this for more accurate locations
        .build()

    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            System.err.println("FusedLocationProvider: Attempted to start updates without permission.")
            _locationUpdatesFlow.value = callbackFlow {
                close(SecurityException("Location permission not granted for FusedLocationProvider."))
            }
            return
        }

        _locationUpdatesFlow.value = callbackFlow {
            val callback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        // TODO FusedLocationProvider might sometimes return older locations first so check location.elapsedRealtimeNanos

                        if (location.hasSpeed()) {
                            val speedMetersPerSecond = location.speed
                            val speedMph = speedMetersPerSecond * 2.23694f // Convert m/s to mph
                            println("FusedLocationProvider: Speed available - ${speedMph} MPH from $location")
                            launch { send(SpeedData(speedMph)) }
                        } else {
                            println("FusedLocationProvider: Location update received, but no speed data. Location: $location")
                        }
                    } ?: println("FusedLocationProvider: onLocationResult - lastLocation was null")

                }
            }
            locationCallback = callback

            println("FusedLocationProvider: PREPARING to call requestLocationUpdates...") // <--- ADD THIS

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            ).addOnFailureListener { e ->
                println("FusedLocationProvider: FAILED to request location updates: ${e.message}") // <---ENSURE THIS IS PRESENT
                close(e)
            }.addOnSuccessListener {
                println("FusedLocationProvider: SUCCESSFULLY started location updates.") // <--- ENSURE THIS IS PRESENT
            }

            println("FusedLocationProvider: FINISHED calling requestLocationUpdates (Task submitted).") // <--- ADD THIS


            awaitClose {
                println("FusedLocationProvider: Stopping location updates.")
                fusedLocationClient.removeLocationUpdates(callback)
                locationCallback = null
            }
        }
        println("FusedLocationProvider: Started location updates flow setup.")
    }

    fun stopLocationUpdates() {
        _locationUpdatesFlow.value = emptyFlow()
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
            println("FusedLocationProvider: Explicitly removed location callback in stopLocationUpdates.")
        }
        println("FusedLocationProvider: Explicitly stopped location updates.")
    }

    companion object {
        private const val MIN_TIME_BW_UPDATES_MS: Long = 1000 // 1 second (desired interval)
        private const val MIN_UPDATE_INTERVAL_MS: Long = 500 // Smallest possible interval
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES_METERS: Float = 0f // Update on any movement
    }
}