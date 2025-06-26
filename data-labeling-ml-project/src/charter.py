import json
import os
import matplotlib
matplotlib.use('TkAgg')
import matplotlib.pyplot as plt
import pandas as pd
from collections import Counter
import math

def load_data(file_path):
    with open(file_path, 'r') as f:
        return json.load(f)

def apply_median_filter(data, window=11):
    df = pd.DataFrame(data)
    numeric_cols = df.select_dtypes(include=['number']).columns
    df[numeric_cols] = df[numeric_cols].rolling(window=window, center=True, min_periods=1).median()
    return df.to_dict(orient='records')

def apply_rolling_average(data, window=25):
    df = pd.DataFrame(data)
    numeric_cols = df.select_dtypes(include=['number']).columns
    df[numeric_cols] = df[numeric_cols].rolling(window=window, center=True, min_periods=1).mean()
    return df.to_dict(orient='records')

def plot_events_by_type(data):
    # Group data by event type
    events = {}
    for entry in data:
        event_type = entry.get('eventType', entry.get('event_type', 'UnknownEvent'))
        events.setdefault(event_type, []).append(entry)

    ignore_fields = {'timestamp', 'eventType', 'event_type', 'gpsUtcTime', 'latitude', 'longitude', 'horizontal_accuracy_m'}

    for event_type, entries in events.items():
        if not entries:
            continue
        # Only include keys present in at least one entry for this event type
        all_keys = set()
        for entry in entries:
            all_keys.update(k for k in entry.keys() if k not in ignore_fields)
        value_keys = list(all_keys)

        plt.figure(figsize=(12, 6))
        for key in value_keys:
            values = [e.get(key, None) for e in entries]
            # Only plot if at least one value is not None and not NaN
            if any(v is not None for v in values):
                plt.plot([e['timestamp'] for e in entries], values, label=key)
        plt.xlabel("Timestamp")
        plt.ylabel("Value")
        plt.title(f"{event_type} Events")
        plt.legend()
        plt.grid(True)
        plt.tight_layout()
        plt.show()

def select_file_from_processed():
    processed_dir = os.path.join("..", "data", "processed")
    files = [f for f in os.listdir(processed_dir) if f.endswith('.json')]
    if not files:
        print("No processed JSON files found.")
        return None
    print("Select a file to chart:")
    for idx, fname in enumerate(files, 1):
        print(f"{idx}: {fname}")
    while True:
        try:
            choice = int(input("Enter the number of the file to chart: "))
            if 1 <= choice <= len(files):
                return os.path.join(processed_dir, files[choice - 1])
            else:
                print("Invalid selection. Try again.")
        except ValueError:
            print("Please enter a valid number.")

if __name__ == "__main__":
    file_path = select_file_from_processed()
    if file_path:
        data = load_data(file_path)
        filtered_data = apply_rolling_average(data)
        filtered_data = apply_median_filter(filtered_data)
        plot_events_by_type(data)