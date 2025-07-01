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

        # Prepare lists for each group
        motion = {'LinearAccelerationEvent': {'x': [], 'y': [], 'z': []},
                  'GyroscopeEvent': {'x': [], 'y': [], 'z': []}}
        pressure = []
        altitude = []

        # Build unified lists for plotting
        for entry in filtered_data:
            ts = entry.get('timestamp')
            if ts is None:
                continue
            etype = entry.get('eventType')
            if etype in motion:
                for axis in ['x', 'y', 'z']:
                    val = entry.get(axis)
                    if val is not None:
                        motion[etype][axis].append((ts, val))
            elif etype == 'BarometerEvent':
                p = entry.get('pressure')
                a = entry.get('altitude')
                if p is not None:
                    pressure.append((ts, p))
                if a is not None:
                    altitude.append((ts, a))

        fig, host_ax = plt.subplots(figsize=(14, 7))
        # Use a high-contrast color palette for better visibility
        high_contrast_colors = [
            '#1f77b4',  # blue
            '#ff7f0e',  # orange
            '#2ca02c',  # green
            '#d62728',  # red
            "#f9fd0e",  # yellow
            "#000000",  # black
            '#e377c2',  # pink
            "#1fd65f",  # light green
            '#bcbd22',  # yellow-green
            '#17becf',  # cyan
        ]
        color_cycle = high_contrast_colors
        axes = [host_ax]
        color_idx = 0
        # Plot motion (x, y, z) for both sensors on host_ax
        for etype in ['LinearAccelerationEvent', 'GyroscopeEvent']:
            for axis in ['x', 'y', 'z']:
                if motion[etype][axis]:
                    ts, vals = zip(*motion[etype][axis])
                    host_ax.plot(ts, vals, label=f'{etype} {axis}', color=color_cycle[color_idx % len(color_cycle)])
                    color_idx += 1
        host_ax.set_ylabel('Motion (x/y/z)')
        host_ax.set_xlabel('Timestamp')
        # Plot pressure on 2nd y-axis if present
        if pressure:
            ts, vals = zip(*pressure)
            ax2 = host_ax.twinx()
            ax2.plot(ts, vals, label='Pressure', color=color_cycle[color_idx % len(color_cycle)])
            ax2.set_ylabel('Pressure')
            axes.append(ax2)
            color_idx += 1
        # Plot altitude on 3rd y-axis if present
        if altitude:
            ts, vals = zip(*altitude)
            ax3 = host_ax.twinx()
            ax3.plot(ts, vals, label='Altitude', color=color_cycle[color_idx % len(color_cycle)])
            ax3.set_ylabel('Altitude')
            ax3.spines['right'].set_position(('axes', 1.1))
            axes.append(ax3)
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