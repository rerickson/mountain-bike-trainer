import json
import matplotlib.pyplot as plt
import numpy as np

# Load nanoTime data from JSON file
with open("session_accel_20250609_183206.json", "r") as f:
    data = json.load(f)

# Get first timestamp to use as a zero reference
start_time_ns = data[0]["timestamp"]

# Convert to relative seconds
times_sec = [(entry["timestamp"] - start_time_ns) / 1e9 for entry in data]
x_vals = [entry["x"] for entry in data]
y_vals = [entry["y"] for entry in data]
z_vals = [entry["z"] for entry in data]

def moving_average(data, window_size=5):
    return np.convolve(data, np.ones(window_size)/window_size, mode='same')

x_vals_smooth = moving_average(x_vals, window_size=10)
y_vals_smooth = moving_average(y_vals, window_size=10)
z_vals_smooth = moving_average(z_vals, window_size=10)

# Parameters (tune as needed)
z_air_threshold = 4.0  # m/s^2, adjust based on your data (gravity ~9.8)
min_air_time = 0.1     # seconds, ignore very short "jumps"

# Find jumps
in_air = False
jumps = []
jump_start = None

for i, z in enumerate(z_vals_smooth):
    if i == 0:
        prev_z = z  # Initialize previous value
        continue
    # Detect transition from above to below threshold (takeoff)
    if not in_air and abs(prev_z) > z_air_threshold and abs(z) < z_air_threshold:
        in_air = True
        jump_start = times_sec[i]
    # Detect transition from below to above threshold (landing)
    elif in_air and abs(prev_z) < z_air_threshold and abs(z) > z_air_threshold:
        in_air = False
        jump_end = times_sec[i]
        air_time = jump_end - jump_start
        if air_time > min_air_time:
            jumps.append((jump_start, jump_end, air_time))
    prev_z = z  # Update previous value

print(f"Number of jumps detected: {len(jumps)}")
for idx, (start, end, air_time) in enumerate(jumps, 1):
    print(f"Jump {idx}: Air time = {air_time:.2f} seconds (from {start:.2f}s to {end:.2f}s)")

# Plotting
plt.figure(figsize=(10, 6))
plt.plot(times_sec, x_vals_smooth, label="X (smoothed)", marker='o')
plt.plot(times_sec, y_vals_smooth, label="Y (smoothed)", marker='o')
plt.plot(times_sec, z_vals_smooth, label="Z (smoothed)", marker='o')

# Optional: Plot detected jumps
for start, end, _ in jumps:
    plt.axvspan(start, end, color='yellow', alpha=0.3, label='Jump' if start == jumps[0][0] else "")

plt.title("Accelerometer Data Over Time")
plt.xlabel("Time Since Start (seconds)")
plt.ylabel("Acceleration")
plt.legend()
plt.grid(True)
plt.tight_layout()
plt.show()
