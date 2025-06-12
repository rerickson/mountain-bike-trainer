def load_json(file_path):
    import json
    with open(file_path, 'r') as f:
        return json.load(f)

def save_json(data, file_path):
    import json
    with open(file_path, 'w') as f:
        json.dump(data, f)

def preprocess_data(raw_data):
    # Implement any necessary preprocessing steps
    return raw_data

def visualize_data(data):
    import matplotlib.pyplot as plt
    plt.plot(data)
    plt.show()

def split_data(data, split_ratio=0.8):
    split_index = int(len(data) * split_ratio)
    return data[:split_index], data[split_index:]