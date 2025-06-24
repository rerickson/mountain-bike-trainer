import json
import os
from rolling_average_filter import apply_rolling_average

def clean_and_process_data(file_path, output_dir, window_size=10):
    """
    Loads raw JSON data, filters for LinearAccelerationEvent and GPSLocationEvent,
    applies a rolling average filter to LinearAccelerationEvent, and saves both event types.
    """
    try:
        with open(file_path, 'r') as f:
            raw_data = json.load(f)
    except (FileNotFoundError, json.JSONDecodeError) as e:
        print(f"Error loading {file_path}: {e}")
        return

    # Separate events
    linear_accel = [
        entry for entry in raw_data
        if entry.get('eventType', '').lower() == 'linearaccelerationevent'
    ]
    gyro = [
        entry for entry in raw_data
        if entry.get('eventType', '').lower() == 'gyroscopeevent'
    ]
    barometer = [
        entry for entry in raw_data
        if entry.get('eventType', '').lower() == 'barometerevent'
    ]
    gps_location = [
        entry for entry in raw_data
        if entry.get('eventType', '').lower() == 'gpslocationevent'
    ]

    # Apply rolling average to linearaccelerationevent and gyro as they are high frequency events
    smoothed_linear_accel = apply_rolling_average(linear_accel, window=window_size) if linear_accel else []
    smoothed_gyro = apply_rolling_average(gyro, window=window_size) if gyro else []
    # smoothed_barometer = apply_rolling_average(barometer, window=window_size) if barometer else []

    # Combine for output
    processed_data = smoothed_linear_accel + gps_location + barometer + smoothed_gyro
    processed_data.sort(key=lambda x: x.get('timestamp', 0))  # Sort by timestamp

    # Save the processed data
    base_name = os.path.basename(file_path).replace('.json', '')
    output_file = os.path.join(output_dir, f"{base_name}_processed.json")
    os.makedirs(output_dir, exist_ok=True)
    try:
        with open(output_file, 'w') as f:
            json.dump(processed_data, f, indent=4)
        print(f"Processed data saved to {output_file}")
    except OSError as e:
        print(f"Error saving processed data to {output_file}: {e}")

def process_all_json_files(data_dir, output_dir, window_size=10):
    """
    Processes all JSON files in the specified directory.

    Args:
        data_dir (str): Path to the directory containing JSON files.
        output_dir (str): Path to the directory where the processed data will be saved.
        window_size (int): The size of the rolling window.
    """
    for filename in os.listdir(data_dir):
        if filename.endswith('.json'):
            file_path = os.path.join(data_dir, filename)
            print(f"Processing {file_path}...")
            clean_and_process_data(file_path, output_dir, window_size=window_size)

if __name__ == "__main__":
    raw_data_dir = os.path.join("..", "data", "raw")
    processed_data_dir = os.path.join("..", "data", "processed")
    process_all_json_files(raw_data_dir, processed_data_dir, window_size=10)
    print("Data processing complete.")