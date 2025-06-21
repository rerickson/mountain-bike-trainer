import json
import os
import math
from rolling_average_filter import apply_rolling_average

def clean_and_process_data(file_path, output_dir, window_size=10):
    """
    Loads raw JSON data, filters for LinearAccelerationEvent, applies a rolling average filter,
    and saves the processed data to a new JSON file.

    Args:
        file_path (str): Path to the raw JSON data file.
        output_dir (str): Path to the directory where the processed data will be saved.
        window_size (int): The size of the rolling window.
    """
    try:
        with open(file_path, 'r') as f:
            raw_data = json.load(f)
    except (FileNotFoundError, json.JSONDecodeError) as e:
        print(f"Error loading {file_path}: {e}")
        return

    # Filter for LinearAccelerationEvent (case-insensitive, handles both eventType and event_type)
    filtered_data = [
        entry for entry in raw_data
        if entry.get('eventType', '').lower() == 'linearaccelerationevent' or entry.get('event_type', '').lower() == 'linearaccelerationevent'
    ]

    if not filtered_data:
        print(f"No LinearAccelerationEvent entries found in {file_path}. Skipping.")
        return

    # Apply rolling average filter
    smoothed_data = apply_rolling_average(filtered_data, window=window_size)

    clean_data = remove_nan_fields(smoothed_data)

    # Save the processed data
    base_name = os.path.basename(file_path).replace('.json', '')
    output_file = os.path.join(output_dir, f"{base_name}_processed.json")
    os.makedirs(output_dir, exist_ok=True)
    try:
        with open(output_file, 'w') as f:
            json.dump(clean_data, f, indent=4)
        print(f"Processed data saved to {output_file}")
    except OSError as e:
        print(f"Error saving processed data to {output_file}: {e}")

def remove_nan_fields(data):
    cleaned = []
    for entry in data:
        cleaned_entry = {k: v for k, v in entry.items() if not (isinstance(v, float) and math.isnan(v))}
        cleaned.append(cleaned_entry)
    return cleaned

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