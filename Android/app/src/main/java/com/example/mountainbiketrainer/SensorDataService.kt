package com.example.mountainbiketrainer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

class SensorDataService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "SensorDataServiceChannel"
        private const val WAKE_LOCK_TAG = "MountainBikeTrainer::SensorDataServiceWakeLock"
        
        // Actions for controlling the service
        const val ACTION_START_COLLECTION = "com.example.mountainbiketrainer.START_COLLECTION"
        const val ACTION_STOP_COLLECTION = "com.example.mountainbiketrainer.STOP_COLLECTION"
        const val ACTION_STOP_SERVICE = "com.example.mountainbiketrainer.STOP_SERVICE"
    }
    
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private lateinit var sensorDataProvider: SensorDataProvider
    private lateinit var locationProvider: LocationProvider
    private lateinit var jumpDetector: JumpDetector
    
    private var sensorCollectionJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    private val allRecordedEvents = mutableListOf<TimestampedSensorEvent>()
    
    // State flows for UI updates
    private val _isCollecting = MutableStateFlow(false)
    val isCollecting: StateFlow<Boolean> = _isCollecting.asStateFlow()
    
    private val _currentSpeed = MutableStateFlow<GPSSpeedEvent?>(null)
    val currentSpeed: StateFlow<GPSSpeedEvent?> = _currentSpeed.asStateFlow()
    
    private val _maxSpeed = MutableStateFlow<GPSSpeedEvent?>(null)
    val maxSpeed: StateFlow<GPSSpeedEvent?> = _maxSpeed.asStateFlow()
    
    private val _maxGForce = MutableStateFlow<Float?>(null)
    val maxGForce: StateFlow<Float?> = _maxGForce.asStateFlow()
    
    private val _lastAirTime = MutableStateFlow<Float?>(null)
    val lastAirTime: StateFlow<Float?> = _lastAirTime.asStateFlow()
    
    private val _currentLinearAccel = MutableStateFlow<LinearAccelEvent?>(null)
    val currentLinearAccel: StateFlow<LinearAccelEvent?> = _currentLinearAccel.asStateFlow()
    
    private val _maxLinearAccel = MutableStateFlow<LinearAccelEvent?>(null)
    val maxLinearAccel: StateFlow<LinearAccelEvent?> = _maxLinearAccel.asStateFlow()
    
    private val _saveFileRequestChannel = Channel<SaveFileRequest>()
    val saveFileRequestFlow = _saveFileRequestChannel.receiveAsFlow()
    
    inner class LocalBinder : Binder() {
        fun getService(): SensorDataService = this@SensorDataService
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d("SensorDataService", "Service created")
        
        sensorDataProvider = SensorDataProvider(this)
        locationProvider = LocationProvider(this)
        jumpDetector = JumpDetector()
        
        createNotificationChannel()
        acquireWakeLock()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SensorDataService", "Service started with intent: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_COLLECTION -> {
                startDataCollection()
            }
            ACTION_STOP_COLLECTION -> {
                stopDataCollection()
            }
            ACTION_STOP_SERVICE -> {
                stopSelf()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d("SensorDataService", "Service destroyed")
        
        stopDataCollection()
        releaseWakeLock()
        serviceScope.cancel()
    }
    
    fun startDataCollection() {
        if (_isCollecting.value) {
            Log.d("SensorDataService", "Data collection already in progress")
            return
        }
        
        _isCollecting.value = true
        allRecordedEvents.clear()
        Log.i("SensorDataService", "Starting data collection")
        
        sensorCollectionJob = serviceScope.launch {
            val sensorFlow = sensorDataProvider.sensorEventsFlow()
            val locationFlow = locationProvider.locationUpdatesFlow()
            
            merge(sensorFlow, locationFlow)
                .collect { event ->
                    if (_isCollecting.value) {
                        allRecordedEvents.add(event)
                        
                        // Process events for UI updates
                        when (event) {
                            is AccelerometerEvent -> {
                                val airTime = jumpDetector.processSensorEvent(event)
                                if (airTime != null) {
                                    _lastAirTime.value = airTime
                                }
                            }
                            is GPSSpeedEvent -> {
                                _currentSpeed.value = event
                                if (_maxSpeed.value == null || event.speedMps > _maxSpeed.value!!.speedMps) {
                                    _maxSpeed.value = event
                                }
                            }
                            is LinearAccelEvent -> {
                                val magnitudeMs2 = sqrt(event.x * event.x + event.y * event.y + event.z * event.z)
                                val gforce = magnitudeMs2 / SensorManager.GRAVITY_EARTH
                                if (_maxGForce.value == null || gforce > _maxGForce.value!!) {
                                    _maxGForce.value = gforce
                                }
                                _currentLinearAccel.value = event
                                if (_maxLinearAccel.value == null || event.z > _maxLinearAccel.value!!.z) {
                                    _maxLinearAccel.value = event
                                }
                            }
                        }
                    }
                }
        }
        
        updateNotification("Recording sensor data...")
    }
    
    fun stopDataCollection() {
        if (!_isCollecting.value) {
            Log.d("SensorDataService", "Data collection not in progress")
            return
        }
        
        _isCollecting.value = false
        sensorCollectionJob?.cancel()
        sensorCollectionJob = null
        Log.i("SensorDataService", "Stopped data collection")
        
        if (allRecordedEvents.isNotEmpty()) {
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val suggestedFileName = "session_raw_all_${sdf.format(Date())}.json"
            
            serviceScope.launch {
                _saveFileRequestChannel.send(
                    SaveFileRequest(
                        suggestedName = suggestedFileName,
                        dataToSave = ArrayList(allRecordedEvents)
                    )
                )
            }
        }
        
        updateNotification("Sensor data collection stopped")
    }
    
    fun resetMaxValues() {
        _maxSpeed.value = null
        _currentSpeed.value = null
        _lastAirTime.value = null
        _maxGForce.value = null
        _maxLinearAccel.value = null
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sensor Data Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when sensor data collection is active"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun updateNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mountain Bike Trainer")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        ).apply {
            acquire(10*60*1000L) // 10 minutes timeout
        }
        Log.d("SensorDataService", "Wake lock acquired")
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d("SensorDataService", "Wake lock released")
            }
        }
        wakeLock = null
    }
} 