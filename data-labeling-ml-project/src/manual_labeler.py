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
    values = [entry[value_key] for entry in data]

    fig, ax = plt.subplots()
    ax.plot(timestamps, values, label=value_key)
    ax.set_title("Click to mark start and end of jumps (pairs)")
    ax.set_xlabel("Timestamp")
    ax.set_ylabel(value_key)
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
        jump_ranges = label_jumps_with_ui(data, value_key="z")
        save_labeled_data(jump_ranges, labeled_data_dir, f"labeled_jumps_{i}.json")

if __name__ == "__main__":
    main()