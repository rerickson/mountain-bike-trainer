import json
import os
import matplotlib
matplotlib.use('TkAgg')
import matplotlib.pyplot as plt
import pandas as pd
from collections import Counter
import math

class CharterPlotter:
    def __init__(self, ignore_fields=None, median_window=11, average_window=25):
        if ignore_fields is None:
            ignore_fields = {'timestamp', 'eventType', 'event_type', 'gpsUtcTime', 'latitude', 'longitude', 'horizontal_accuracy_m','pressure'}
        self.ignore_fields = ignore_fields
        self.median_window = median_window
        self.average_window = average_window

    def load_data(self, file_path):
        with open(file_path, 'r') as f:
            return json.load(f)

    def apply_median_filter(self, data, window=None):
        if window is None:
            window = self.median_window
        df = pd.DataFrame(data)
        numeric_cols = df.select_dtypes(include=['number']).columns
        df[numeric_cols] = df[numeric_cols].rolling(window=window, center=True, min_periods=1).median()
        return df.to_dict(orient='records')

    def apply_rolling_average(self, data, window=None):
        if window is None:
            window = self.average_window
        df = pd.DataFrame(data)
        numeric_cols = df.select_dtypes(include=['number']).columns
        df[numeric_cols] = df[numeric_cols].rolling(window=window, center=True, min_periods=1).mean()
        return df.to_dict(orient='records')

    def plot_events_by_type(self, data):
        events = {}
        for entry in data:
            event_type = entry.get('eventType', entry.get('event_type', 'UnknownEvent'))
            events.setdefault(event_type, []).append(entry)

        for event_type, entries in events.items():
            if not entries:
                continue
            all_keys = set()
            for entry in entries:
                all_keys.update(k for k in entry.keys() if k not in self.ignore_fields)
            value_keys = list(all_keys)

            plt.figure(figsize=(12, 6))
            for key in value_keys:
                values = [e.get(key, None) for e in entries]
                if any(v is not None for v in values):
                    plt.plot([e['timestamp'] for e in entries], values, label=key)
            plt.xlabel("Timestamp")
            plt.ylabel("Value")
            plt.title(f"{event_type} Events")
            plt.legend()
            plt.grid(True)
            plt.tight_layout()
            plt.show()

    def plot_events_with_charter(self, data, file_name=None):
        filtered_data = self.apply_rolling_average(data)
        filtered_data = self.apply_median_filter(filtered_data)

        events = {}
        for entry in filtered_data:
            event_type = entry.get('eventType', entry.get('event_type', 'UnknownEvent'))
            events.setdefault(event_type, []).append(entry)

        fig, host_ax = plt.subplots(figsize=(14, 7))
        axes = [host_ax]
        color_cycle = plt.rcParams['axes.prop_cycle'].by_key()['color']
        used_keys = set()
        # Group gyro and linear acceleration keys
        gyro_keys = {'gyroscope_x', 'gyroscope_y', 'gyroscope_z'}
        accel_keys = {'linear_acceleration_x', 'linear_acceleration_y', 'linear_acceleration_z'}
        combined_keys = gyro_keys | accel_keys
        # Plot combined gyro/accel on host_ax
        for event_type, entries in events.items():
            if not entries:
                continue
            all_keys = set()
            for entry in entries:
                all_keys.update(k for k in entry.keys() if k not in self.ignore_fields)
            value_keys = list(all_keys)
            # Plot combined gyro/accel
            for key in value_keys:
                if key in combined_keys and key not in used_keys:
                    values = [e.get(key, None) for e in entries]
                    if any(v is not None for v in values):
                        color = color_cycle[len(used_keys) % len(color_cycle)]
                        host_ax.plot([e['timestamp'] for e in entries], values, label=key, color=color)
                        used_keys.add(key)
        # Plot all other keys on separate axes
        for event_type, entries in events.items():
            if not entries:
                continue
            all_keys = set()
            for entry in entries:
                all_keys.update(k for k in entry.keys() if k not in self.ignore_fields)
            value_keys = list(all_keys)
            for key in value_keys:
                if key in combined_keys:
                    continue  # Already plotted
                values = [e.get(key, None) for e in entries]
                if not any(v is not None for v in values):
                    continue
                ax = host_ax.twinx()
                if len(axes) > 1:
                    ax.spines["right"].set_position(("axes", 1 + 0.1 * (len(axes) - 1)))
                axes.append(ax)
                color = color_cycle[(len(axes)-1) % len(color_cycle)]
                ax.plot([e['timestamp'] for e in entries], values, label=f"{event_type}: {key}", color=color)
                ax.set_ylabel(f"{event_type}: {key}", color=color)
                ax.tick_params(axis='y', labelcolor=color)
        host_ax.set_xlabel("Timestamp")
        host_ax.set_ylabel("Gyro/Accel Values")
        title = f"Events" if file_name is None else f"Events: {file_name}"
        host_ax.set_title(title)
        # Combine all legends
        lines, labels = [], []
        for ax in axes:
            line, label = ax.get_legend_handles_labels()
            lines += line
            labels += label
        host_ax.legend(lines, labels, loc='upper left', bbox_to_anchor=(1.05, 1))
        host_ax.grid(True)
        plt.tight_layout(rect=[0, 0, 0.85, 1])
        return host_ax

    def select_file_from_processed(self):
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
    plotter = CharterPlotter()
    file_path = plotter.select_file_from_processed()
    if file_path:
        data = plotter.load_data(file_path)
        filtered_data = plotter.apply_rolling_average(data)
        filtered_data = plotter.apply_median_filter(filtered_data)
        plotter.plot_events_by_type(data)