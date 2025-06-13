import os
import json
import matplotlib.pyplot as plt

def load_raw_data(raw_data_dir):
    raw_data = []
    for filename in os.listdir(raw_data_dir):
        if filename.endswith('.json'):
            with open(os.path.join(raw_data_dir, filename), 'r') as f:
                raw_data.append(json.load(f))
    return raw_data

def label_jumps_with_ui(data, value_key="z"):
    timestamps = [entry["timestamp"] for entry in data]
    x_values = [entry.get("x", 0) for entry in data]
    y_values = [entry.get("y", 0) for entry in data]
    z_values = [entry.get("z", 0) for entry in data]

    fig, ax = plt.subplots()
    ax.plot(timestamps, x_values, label="x", alpha=0.5)
    ax.plot(timestamps, y_values, label="y", alpha=0.5)
    ax.plot(timestamps, z_values, label="z", alpha=0.8, linewidth=2 if value_key == "z" else 1.2)
    ax.set_title(f"Click to mark start and end of jumps (pairs) - Labeling: {value_key}")
    ax.set_xlabel("Timestamp")
    ax.set_ylabel("Value")
    plt.legend()

    clicks = []
    jump_ranges = []

    def onclick(event):
        if event.inaxes != ax:
            return
        clicks.append(event.xdata)
        ax.axvline(event.xdata, color='red')
        plt.draw()
        if len(clicks) % 2 == 0:
            start = clicks[-2]
            end = clicks[-1]
            print(f"Marked jump: {start} to {end}")
            jump_ranges.append({"start": start, "end": end})

    cid = fig.canvas.mpl_connect('button_press_event', onclick)
    plt.show()
    fig.canvas.mpl_disconnect(cid)
    return jump_ranges

def save_labeled_data(jump_ranges, labeled_data_dir, filename):
    if not os.path.exists(labeled_data_dir):
        os.makedirs(labeled_data_dir)
    out_path = os.path.join(labeled_data_dir, filename)
    with open(out_path, 'w') as f:
        json.dump(jump_ranges, f, indent=2)
    print(f"Labeled data saved to {out_path}")

def main():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    raw_data_dir = os.path.normpath(os.path.join(base_dir, '..', 'data'))
    labeled_data_dir = os.path.normpath(os.path.join(base_dir, '..', 'labeled_data'))

    raw_data_files = load_raw_data(raw_data_dir)
    for i, data in enumerate(raw_data_files):
        print(f"Labeling file {i+1}/{len(raw_data_files)}")
        while True:
            jump_ranges = label_jumps_with_ui(data, value_key="z")
            print(f"\nYou marked {len(jump_ranges)} jump(s).")
            user_input = input("Press [Enter] to accept, or type 'r' to redo labeling: ").strip().lower()
            if user_input == "r":
                print("Restarting labeling for this file...")
                continue
            save_labeled_data(jump_ranges, labeled_data_dir, f"labeled_jumps_{i}.json")
            break

if __name__ == "__main__":
    main()