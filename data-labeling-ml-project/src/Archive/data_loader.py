import os
import pandas as pd
import json

def load_raw_data(data_dir):
    raw_data = []
    for filename in os.listdir(data_dir):
        if filename.endswith('.json'):
            file_path = os.path.join(data_dir, filename)
            with open(file_path, 'r') as f:
                json_data = json.load(f)
                # Ensure json_data is a list
                if isinstance(json_data, list):
                    for entry in json_data:
                        # Ensure each entry is a list of [event_type, event_dict]
                        if isinstance(entry, list) and len(entry) == 2:
                            event_type_full, event_dict = entry
                            event_name = event_type_full.split('.')[-1]
                            flat = dict(event_dict)
                            flat['event_name'] = event_name
                            raw_data.append(flat)
                        else:
                            print(f"Warning: Skipping malformed entry: {entry}")
                else:
                    print(f"Warning: Skipping non-list JSON data in {filename}")
        elif filename.endswith('.csv'):
            file_path = os.path.join(data_dir, filename)
            data = pd.read_csv(file_path)
            raw_data.append(data)
    return raw_data  # Now a list of dicts

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