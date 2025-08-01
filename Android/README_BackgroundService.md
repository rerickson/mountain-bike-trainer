# Background Service Implementation

This document describes the background service implementation for the Mountain Bike Trainer app that allows continuous sensor data collection even when the app is in the background.

## Overview

The app now uses a `SensorDataService` that runs as a foreground service to collect sensor data continuously. This service:

- Runs in the background with a persistent notification
- Uses wake locks to prevent the device from sleeping
- Collects high-frequency sensor data using `SENSOR_DELAY_FASTEST`
- Maintains the same data processing logic as before
- Provides real-time updates to the UI through StateFlow

## Key Components

### SensorDataService
- **Location**: `Android/app/src/main/java/com/example/mountainbiketrainer/SensorDataService.kt`
- **Purpose**: Foreground service that handles sensor data collection
- **Features**:
  - Foreground service with notification
  - Wake lock support for continuous monitoring
  - Sensor data collection from multiple sensors
  - GPS location tracking
  - Jump detection and air time calculation
  - Real-time statistics (max speed, max G-force, etc.)

### Updated MainViewModel
- **Location**: `Android/app/src/main/java/com/example/mountainbiketrainer/MainViewModel.kt`
- **Changes**:
  - Removed direct sensor data collection logic
  - Now binds to `SensorDataService` and delegates operations
  - Maintains the same public API for UI components
  - Observes service state flows for real-time updates

## Permissions

The following permissions are required and already declared in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.HIGH_SAMPLING_RATE_SENSORS" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

**Note**: The `FOREGROUND_SERVICE_LOCATION` permission is required for Android 14+ (API 34+) when using a foreground service with location type. This permission is automatically requested along with location permissions.

## Service Lifecycle

1. **Service Creation**: The service is automatically started when the MainViewModel is created
2. **Service Binding**: MainViewModel binds to the service to communicate with it
3. **Data Collection**: When the user starts collection, the service begins collecting sensor data
4. **Background Operation**: The service continues running even when the app is backgrounded
5. **Service Cleanup**: The service is properly cleaned up when the ViewModel is destroyed

## Usage

The service is automatically managed by the MainViewModel. Users don't need to interact with it directly. The existing UI will work exactly as before, but now with background capabilities.

### Starting Data Collection
```kotlin
// This is handled automatically by the existing UI
mainViewModel.toggleOverallDataCollection()
```

### Stopping Data Collection
```kotlin
// This is handled automatically by the existing UI
mainViewModel.toggleOverallDataCollection()
```

## Benefits

1. **Continuous Data Collection**: Sensor data is collected even when the app is in the background
2. **Battery Optimization**: Uses wake locks efficiently with timeouts
3. **User Awareness**: Persistent notification shows when data collection is active
4. **Seamless Integration**: No changes required to existing UI components
5. **Data Integrity**: All sensor events are captured and processed in real-time

## Technical Details

### Wake Lock Management
- Uses `PARTIAL_WAKE_LOCK` to keep the CPU running
- 10-minute timeout to prevent excessive battery drain
- Automatically released when the service is destroyed

### Notification
- Low-priority notification to avoid being intrusive
- Shows current status (recording/stopped)
- Required for foreground services on Android

### Sensor Configuration
- Uses `SENSOR_DELAY_FASTEST` for maximum data collection frequency
- Collects from all available sensors: accelerometer, gyroscope, linear acceleration, magnetic field, pressure, gravity, rotation vector
- GPS location updates with high accuracy

## Troubleshooting

### Service Not Starting
- Check that all required permissions are granted
- Verify the service is properly declared in AndroidManifest.xml
- Check logcat for any service-related errors

### Data Not Being Collected
- Ensure location permissions are granted
- Check that the device has the required sensors
- Verify the service is bound and running

### Battery Drain
- The service uses wake locks efficiently with timeouts
- Monitor battery usage in device settings
- Consider adjusting wake lock timeout if needed

## Future Enhancements

1. **Adaptive Sampling**: Adjust sensor sampling rate based on activity
2. **Data Compression**: Compress sensor data to reduce storage requirements
3. **Cloud Sync**: Upload data to cloud storage in real-time
4. **Power Management**: Implement more sophisticated power management strategies 