import os
import pandas as pd

def load_raw_data(data_dir):
    raw_data = []
    for filename in os.listdir(data_dir):
        if filename.endswith('.csv'):
            file_path = os.path.join(data_dir, filename)
            data = pd.read_csv(file_path)
            raw_data.append(data)
        elif filename.endswith('.json'):
            file_path = os.path.join(data_dir, filename)
            data = pd.read_json(file_path)
            raw_data.append(data)
        # Add more file formats as needed
    return pd.concat(raw_data, ignore_index=True)

def preprocess_data(data):
    # Implement any necessary preprocessing steps here
    # For example, handling missing values, normalizing, etc.
    return data.dropna()  # Example: dropping rows with missing values

def get_data_shape(data):
    return data.shape

def save_preprocessed_data(data, output_path):
    data.to_csv(output_path, index=False)  # Save as CSV for simplicity

def load_labeled_data(labeled_data_dir):
    labeled_data = []
    for filename in os.listdir(labeled_data_dir):
        if filename.endswith('.csv'):
            file_path = os.path.join(labeled_data_dir, filename)
            data = pd.read_csv(file_path)
            labeled_data.append(data)
    return pd.concat(labeled_data, ignore_index=True) if labeled_data else None