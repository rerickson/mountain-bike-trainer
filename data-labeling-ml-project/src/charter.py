import json
import os
import matplotlib
matplotlib.use('TkAgg')
import matplotlib.pyplot as plt
import pandas as pd

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

    for event_type, entries in events.items():
        if not entries:
            continue
        timestamps = [e['timestamp'] for e in entries]
        value_keys = [k for k in entries[0].keys() if k not in ('timestamp', 'eventType', 'event_type')]
        plt.figure(figsize=(12, 6), dpi=150)
        downsampled = entries  # Plot every 10th point
        for key in value_keys:
            values = [e.get(key, 0) for e in downsampled]
            plt.plot(timestamps, values, label=key, linewidth=1, antialiased=True)
        raw_values = [e.get(key, 0) for e in entries]
        smoothed_values = [e.get(key, 0) for e in filtered_data]
        # plt.plot(timestamps, raw_values, alpha=0.3, label='Raw')
        plt.plot(timestamps, smoothed_values, linewidth=2, label='Smoothed')
        plt.xlabel("Timestamp")
        plt.ylabel("Value")
        plt.title(f"{event_type} Events")
        plt.legend()
        plt.grid(True)
        plt.tight_layout()
        # Ensure the plots directory exists
        plots_dir = os.path.join(os.path.dirname(__file__), '..', 'plots')
        os.makedirs(plots_dir, exist_ok=True)
        # When saving the plot:
        file_name = f"{event_type}_events.png"
        out_path = os.path.join(plots_dir, file_name)
        plt.savefig(out_path)
        plt.show()
        plt.close()
        print(f"Saved chart: {out_path}")

if __name__ == "__main__":
    file_path = input("Enter the path to your JSON file: ").strip()
    data = load_data(file_path)
    filtered_data = apply_median_filter(data)
    filtered_data = apply_rolling_average(data, window=25)
    plot_events_by_type(filtered_data)