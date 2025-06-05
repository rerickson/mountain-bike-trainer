import pandas as pd
import matplotlib.pyplot as plt

# Load the CSV file
try:
    # Adjust column names if your header is different
    df = pd.read_csv('accel_recording2.csv')
except FileNotFoundError:
    print("Error: accel_recording.csv not found. Make sure it's in the same directory as the script.")
    exit()
except Exception as e:
    print(f"Error loading CSV: {e}")
    exit()

if df.empty:
    print("CSV file is empty.")
    exit()

# Ensure required columns exist
required_columns = ['timestamp_ns', 'accel_x', 'accel_y', 'accel_z']
if not all(col in df.columns for col in required_columns):
    print(f"CSV must contain columns: {', '.join(required_columns)}")
    print(f"Found columns: {df.columns.tolist()}")
    exit()


# Convert timestamp from nanoseconds to seconds for a more readable x-axis
# Create a relative time axis starting from 0
df['time_s'] = (df['timestamp_ns'] - df['timestamp_ns'].iloc[0]) / 1_000_000_000.0

# Create the plot
plt.figure(figsize=(15, 8))

plt.plot(df['time_s'], df['accel_x'], label='Accel X')
plt.plot(df['time_s'], df['accel_y'], label='Accel Y')
plt.plot(df['time_s'], df['accel_z'], label='Accel Z (Vertical Focus)')

# Add a plot for magnitude (optional, but often useful)
# df['accel_magnitude'] = (df['accel_x']**2 + df['accel_y']**2 + df['accel_z']**2)**0.5
# plt.plot(df['time_s'], df['accel_magnitude'], label='Accel Magnitude', linestyle=':')


plt.title('Linear Accelerometer Data Over Time')
plt.xlabel('Time (seconds from start of recording)')
plt.ylabel('Acceleration (m/s^2)')
plt.legend()
plt.grid(True)
plt.tight_layout()
plt.show()