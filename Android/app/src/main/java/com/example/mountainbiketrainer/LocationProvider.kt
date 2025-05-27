// In LocationProvider.kt

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

data class SpeedData(val speedMph: Float)

class LocationProvider(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var locationListener: LocationListener? = null // Keep a reference to remove it

    // Use a MutableStateFlow to hold the flow of updates.
    // This allows us to switch to an active flow only when started.
    private val _locationUpdatesFlow = MutableStateFlow<Flow<SpeedData>>(emptyFlow())
    val locationUpdates: Flow<SpeedData> = _locationUpdatesFlow.asStateFlow().value // Expose the current flow

    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // This should ideally not be reached if ViewModel calls this only after permission grant
            System.err.println("LocationProvider: Attempted to start updates without permission.")
            _locationUpdatesFlow.value = callbackFlow { close(SecurityException("Location permission not granted.")) }
            return
        }

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            System.err.println("LocationProvider: GPS provider is not enabled.")
            _locationUpdatesFlow.value = callbackFlow { close(IllegalStateException("GPS provider is not enabled.")) }
            return
        }

        // Create the actual flow that emits location data
        _locationUpdatesFlow.value = callbackFlow {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (location.hasSpeed()) {
                        val speedMetersPerSecond = location.speed
                        val speedMph = speedMetersPerSecond * 2.23694f
                        launch { send(SpeedData(speedMph)) }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {
                    close(IllegalStateException("Location provider $provider disabled"))
                }
            }
            locationListener = listener // Store for removal

            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    listener,
                    Looper.getMainLooper()
                )
            } catch (e: Exception) {
                close(e)
            }

            awaitClose {
                println("LocationProvider: Stopping location updates.")
                locationManager.removeUpdates(listener)
                locationListener = null
            }
        }
        println("LocationProvider: Started location updates.")
    }

    fun stopLocationUpdates() {
        // To explicitly stop, we can make the flow complete or switch to an empty flow
        // The awaitClose in the callbackFlow will handle removing the listener.
        // We might want to make _locationUpdatesFlow a conflated channel or similar
        // if we want to "send" a stop signal, but for now, making it an empty flow
        // when explicitly stopped is fine. The collection will just stop.
        _locationUpdatesFlow.value = emptyFlow()
        locationListener?.let {
            locationManager.removeUpdates(it) // Ensure cleanup if awaitClose wasn't triggered by scope cancellation
            locationListener = null
        }
        println("LocationProvider: Explicitly stopped location updates.")
    }


    companion object {
        private const val MIN_TIME_BW_UPDATES: Long = 1000 * 1 // 1 second
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Float = 0f
    }
}