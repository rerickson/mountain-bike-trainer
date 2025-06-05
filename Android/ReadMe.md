
1. **Open in Android Studio:**
    *   Open Android Studio.
    *   Select "Open" and navigate to the cloned project directory.
    *   Allow Android Studio to sync Gradle and download dependencies.
2. **Permissions:**
    *   The app requires Location permissions (`ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`) to track speed and location.
    *   (Future) It may require `HIGH_SAMPLING_RATE_SENSORS` permission for more precise sensor data on supported devices.
3. **Run the app:**
    *   Select a target device/emulator.
    *   Click the "Run" button (green play icon) in Android Studio.
    *   Grant necessary permissions when prompted by the app.

## How to Use

1.  Launch the app.
2.  Grant Location permissions if prompted.
3.  Press the **"Start"** button to begin collecting telemetry data.
4.  Perform your mountain biking activities. The app will display real-time speed, G-forces, and attempt to detect air time from jumps.
5.  Press the **"Stop"** button to pause data collection.
6.  Use the **"Reset"** button to clear maximum speed and G-force readings for the current session.

## Current Status & Future Development

*   Basic tracking of speed, max speed, and max G-forces
*   Experimental jump air time detection is in early stages and requires further refinement and testing.

**Planned Features / Areas for Improvement:**

*   Pitch and Roll detection during a jump
*   More robust jump detection algorithms (potentially incorporating ML).
*   Calculation and display of jump height and distance (challenging).
*   Detailed jump analysis (takeoff Gs, landing Gs, orientation).
*   Saving and reviewing past sessions.

## Known Issues

*   Jump detection (Air Time) is experimental and may not be accurate in all conditions. Thresholds need tuning.
*   GPS speed accuracy can vary depending on signal strength and environment.
*   (Add any other known issues you've encountered)
