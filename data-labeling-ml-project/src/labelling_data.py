import os
import json
import matplotlib.pyplot as plt
from charter import CharterPlotter

plotter = CharterPlotter()

def load_processed_data(file_path):
    with open(file_path, 'r') as f:
        return json.load(f)

def onclick(event, jump_ranges, axes):
    # Accept clicks on any axis in the axes list
    if event.inaxes in axes:
        timestamp = event.xdata
        print(f"Clicked timestamp: {timestamp}")
        color = 'r' if not jump_ranges or len(jump_ranges[-1]) == 2 else 'g'
        if not jump_ranges or len(jump_ranges[-1]) == 2:
            jump_ranges.append([timestamp])
            event.inaxes.axvline(x=timestamp, color=color, linestyle='--')
        elif len(jump_ranges[-1]) == 1:
            jump_ranges[-1].append(timestamp)
            event.inaxes.axvline(x=timestamp, color=color, linestyle='--')
        plt.draw()

def save_labels(labels_dir, base_name, jump_ranges):
    os.makedirs(labels_dir, exist_ok=True)
    # Ensure start < end for each jump
    sorted_ranges = [
        {"start": min(jump), "end": max(jump)}
        for jump in jump_ranges if len(jump) == 2
    ]
    out_path = os.path.join(labels_dir, f"{base_name}_labels.json")
    with open(out_path, 'w') as f:
        json.dump(sorted_ranges, f, indent=4)
    print(f"Labels saved to {out_path}")

def label_jumps_in_file(file_path, labels_dir):
    data = load_processed_data(file_path)
    file_name = os.path.basename(file_path)
    print(f"\nLabeling jumps for: {file_name}")

    ax = plotter.plot_events_with_charter(data, file_name)
    fig = ax.get_figure()
    axes = fig.get_axes()  # Get all axes, including twinx

    jump_ranges = []
    cid = fig.canvas.mpl_connect('button_press_event', lambda event: onclick(event, jump_ranges, axes))
    plt.show(block=True)  # Ensure the plot is blocking and interactive
    fig.canvas.mpl_disconnect(cid)

    base_name = os.path.splitext(file_name)[0]
    save_labels(labels_dir, base_name, jump_ranges)

def main():
    processed_dir = os.path.join("..", "data", "processed")
    labels_dir = os.path.join("..", "data", "labeled")
    files = [f for f in os.listdir(processed_dir) if f.endswith('.json')]
    for file_name in files:
        file_path = os.path.join(processed_dir, file_name)
        label_jumps_in_file(file_path, labels_dir)

if __name__ == "__main__":
    main()