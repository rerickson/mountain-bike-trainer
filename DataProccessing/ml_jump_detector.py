import json
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.tree import DecisionTreeClassifier
from sklearn.metrics import classification_report
import matplotlib.pyplot as plt

# Load data
with open("data/session_accel_20250609_183206.json", "r") as f:
    data = json.load(f)

# Prepare data arrays
x_vals = np.array([entry["x"] for entry in data])
y_vals = np.array([entry["y"] for entry in data])
z_vals = np.array([entry["z"] for entry in data])

# --- Step 1: Create sliding windows as features ---
window_size = 20  # Number of samples per window (tune as needed)
stride = 5        # Step size between windows

features = []
indices = []

for start in range(0, len(z_vals) - window_size, stride):
    end = start + window_size
    window = np.stack([x_vals[start:end], y_vals[start:end], z_vals[start:end]], axis=1)
    features.append(window.flatten())
    indices.append(start + window_size // 2)  # Center index of window

features = np.array(features)

# --- Step 2: Label your data ---
# For a real ML model, you need ground truth labels (0 = not jump, 1 = jump)
# For now, we'll create dummy labels (all zeros) as a placeholder
# Replace this with your own labeling process!
# Example: Suppose you know jumps occurred between 6.0-6.5s and 8.0-8.3s
# Replace these with your actual jump intervals (in seconds)
jump_ranges = [
    (6.0, 6.5),
    (8.0, 8.3)
]

# Get timestamps for each window's center
times_sec = np.array([entry["timestamp"] for entry in data])
start_time_ns = times_sec[0]
times_sec = (times_sec - start_time_ns) / 1e9
window_centers = [times_sec[idx] for idx in indices]

# Label windows: 1 if center is in a jump range, else 0
labels = np.zeros(len(features), dtype=int)
for i, t in enumerate(window_centers):
    for start, end in jump_ranges:
        if start <= t <= end:
            labels[i] = 1
            break

# --- Step 3: Train/test split ---
X_train, X_test, y_train, y_test = train_test_split(features, labels, test_size=0.2, random_state=42)

# --- Step 4: Train a simple classifier ---
clf = DecisionTreeClassifier()
clf.fit(X_train, y_train)

# --- Step 5: Evaluate ---
y_pred = clf.predict(X_test)
print(classification_report(y_test, y_pred))

# --- Step 6: Predict on all windows (for plotting or further analysis) ---
predictions = clf.predict(features)

# --- Interactive labeling ---
jump_ranges = []

fig, ax = plt.subplots()
ax.plot([entry["timestamp"] for entry in data], z_vals, label="Z Accel")
ax.set_title("Click to mark start and end of jumps (pairs)")
ax.set_xlabel("Timestamp (ns)")
ax.set_ylabel("Z Acceleration")
plt.legend()

clicks = []

def onclick(event):
    if event.inaxes != ax:
        return
    clicks.append(event.xdata)
    print(f"Clicked at: {event.xdata}")
    # Draw a vertical line where clicked
    ax.axvline(event.xdata, color='red')
    plt.draw()
    # When two clicks are made, save as a jump range
    if len(clicks) % 2 == 0:
        start = clicks[-2]
        end = clicks[-1]
        print(f"Jump range: {start} to {end}")
        jump_ranges.append((start, end))

cid = fig.canvas.mpl_connect('button_press_event', onclick)
plt.show()

# Convert jump_ranges from timestamp (ns) to seconds relative to start
start_time_ns = data[0]["timestamp"]
jump_ranges_sec = [((s - start_time_ns) / 1e9, (e - start_time_ns) / 1e9) for s, e in jump_ranges]

print("Jump ranges (seconds):", jump_ranges_sec)
# Now use jump_ranges_sec in your labeling code

print("ML pipeline ready. Next step: label your data with real jump events!")