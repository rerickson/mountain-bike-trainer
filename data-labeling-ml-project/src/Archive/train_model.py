import os
import json
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.tree import DecisionTreeClassifier
from sklearn.metrics import classification_report

def load_labeled_data(labeled_data_dir):
    labeled_data = []
    for filename in os.listdir(labeled_data_dir):
        if filename.endswith('.json'):
            with open(os.path.join(labeled_data_dir, filename), 'r') as f:
                labeled_data.append(json.load(f))
    return labeled_data

def prepare_features_and_labels(labeled_data):
    features = []
    labels = []
    for entry in labeled_data:
        features.append(entry['features'])
        labels.append(entry['label'])
    return np.array(features), np.array(labels)

def train_model(features, labels):
    X_train, X_test, y_train, y_test = train_test_split(features, labels, test_size=0.2, random_state=42)
    clf = DecisionTreeClassifier()
    clf.fit(X_train, y_train)
    y_pred = clf.predict(X_test)
    print(classification_report(y_test, y_pred))
    return clf

def main():
    labeled_data_dir = '../labeled_data'
    labeled_data = load_labeled_data(labeled_data_dir)
    features, labels = prepare_features_and_labels(labeled_data)
    model = train_model(features, labels)
    print("Model training complete.")

if __name__ == "__main__":
    main()