import json
import matplotlib.pyplot as plt

# Load nanoTime data from JSON file
with open("session_accel_20250608_182417.json", "r") as f:
    data = json.load(f)

# Get first timestamp to use as a zero reference
start_time_ns = data[0]["timestamp"]

# Convert to relative seconds
times_sec = [(entry["timestamp"] - start_time_ns) / 1e9 for entry in data]
x_vals = [entry["x"] for entry in data]
y_vals = [entry["y"] for entry in data]
z_vals = [entry["z"] for entry in data]

# Plotting
plt.figure(figsize=(10, 6))
plt.plot(times_sec, x_vals, label="X", marker='o')
plt.plot(times_sec, y_vals, label="Y", marker='o')
plt.plot(times_sec, z_vals, label="Z", marker='o')

plt.title("Accelerometer Data Over Time")
plt.xlabel("Time Since Start (seconds)")
plt.ylabel("Acceleration")
plt.legend()
plt.grid(True)
plt.tight_layout()
plt.show()
