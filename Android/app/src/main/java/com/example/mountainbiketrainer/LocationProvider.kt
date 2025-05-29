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
    private var locationListener: LocationListener? = null

    private val _locationUpdatesFlow = MutableStateFlow<Flow<SpeedData>>(emptyFlow())
    val locationUpdates: Flow<SpeedData> = _locationUpdatesFlow.asStateFlow().value

    fun startLocationUpdates() {
        // Create the actual flow that emits location data
        _locationUpdatesFlow.value = callbackFlow @androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION]) {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    println("LocationProvider: Location changed")

                    if (location.hasSpeed()) {
                        val speedMetersPerSecond = location.speed
                        val speedMph = speedMetersPerSecond * 2.23694f
                        System.err.println("LocationProvider: Found speed of: " + speedMph)
                        println("LocationProvider: Speed found")

                        launch { send(SpeedData(speedMph)) }
                    } else {

                        System.err.println("LocationProvider: Location does not have speed")
                        println("LocationProvider: Does not have speed")
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                    println("LocationProvider: Status changed")
                }
                override fun onProviderEnabled(provider: String) {
                    println("LocationProvider: Provider enabled")
                }
                override fun onProviderDisabled(provider: String) {
                    close(IllegalStateException("Location provider $provider disabled"))
                }
            }
            locationListener = listener

            println("LocationProvider: Adding Listener")
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    listener,
                    Looper.getMainLooper()
                )
            } catch (e: Exception) {
                println("LocationProvider: Started location updates with error: " + e)
                close(e)
            }

            awaitClose {
                println("LocationProvider: Stopping location updates.")
                locationManager.removeUpdates(listener)
                locationListener = null
            }
        }

        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        println("LocationProvider: GPS enabled: " + gpsEnabled)
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