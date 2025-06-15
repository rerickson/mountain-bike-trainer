import json
import matplotlib.pyplot as plt
import os

def process_raw_data(file_path):
    """
    Processes the raw JSON data to extract event types and their corresponding data.

    Args:
    file_path (str): Path to the raw JSON data file.

    Returns:
    dict: A dictionary where keys are event types and values are lists of event data.
    """
    event_data = {}
    with open(file_path, 'r') as f:
        raw_data = json.load(f)

    for entry in raw_data:
        event_type = entry.get('eventType', 'UnknownEvent')  # Extract event type
        if event_type not in event_data:
            event_data[event_type] = []
        event_data[event_type].append(entry)

    return event_data

def display_event_data(event_data, output_dir="plots"):
    """
    Displays all fields within each event type on a single plot, with timestamp on the x-axis.

    Args:
    event_data (dict): A dictionary where keys are event types and values are lists of event data.
    output_dir (str): Directory to save the plots.
    """
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    for event_type, data_points in event_data.items():
        # Extract timestamps and all other fields
        timestamps = [dp.get('timestamp', i) for i, dp in enumerate(data_points)]  # Use index if timestamp is missing
        all_keys = set()
        for dp in data_points:
            all_keys.update(dp.keys())
        all_keys.discard('timestamp')  # Don't plot timestamp against itself
        all_keys.discard('eventType') # Don't plot eventType

        # Create the plot
        plt.figure(figsize=(12, 8))  # Adjust figure size for better readability

        # Plot each field against the timestamp
        for key in all_keys:
            values = [dp.get(key, None) for dp in data_points]
            valid_data = [(t, v) for t, v in zip(timestamps, values) if v is not None and isinstance(v, (int, float))]

            if not valid_data:
                print(f"Skipping {key} in {event_type}: No valid numeric data to plot.")
                continue

            ts, vals = zip(*valid_data)  # Unzip the valid data points
            plt.plot(ts, vals, label=key)

        plt.xlabel("Timestamp" if 'timestamp' in all_keys else "Sample Index")
        plt.ylabel("Value")
        plt.title(f"{event_type} - All Fields")
        plt.grid(True)
        plt.legend()  # Show legend to identify each field

        # Save the plot
        file_name = f"{event_type}_AllFields.png"
        file_path = os.path.join(output_dir, file_name)
        plt.savefig(file_path)
        plt.close()

        print(f"Saved plot to {file_path}")

if __name__ == "__main__":
    file_path = '../data/session_raw_all_20250613_130555.json'  # Replace with your file path
    processed_data = process_raw_data(file_path)
    display_event_data(processed_data)
    print("Plots generated successfully in the 'plots' directory.")