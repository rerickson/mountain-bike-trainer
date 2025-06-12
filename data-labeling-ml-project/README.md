# Data Labeling ML Project

This project is designed to facilitate the manual labeling of raw data and subsequently train a machine learning model using the labeled data. Below are the details of the project structure and instructions for usage.

## Project Structure

```
data-labeling-ml-project
├── data
│   └── raw                # Directory containing raw data files for labeling
├── labeled_data           # Directory for storing labeled data files
├── src                    # Source code directory
│   ├── data_loader.py     # Functions to load and preprocess raw data
│   ├── manual_labeler.py   # User interface for manual labeling of data
│   ├── train_model.py     # Training process for the machine learning model
│   └── utils.py           # Utility functions for various tasks
├── requirements.txt       # List of project dependencies
└── README.md              # Project documentation
```

## Setup Instructions

1. **Clone the Repository**: 
   Clone this repository to your local machine using:
   ```
   git clone <repository-url>
   ```

2. **Install Dependencies**: 
   Navigate to the project directory and install the required packages using:
   ```
   pip install -r requirements.txt
   ```

3. **Prepare Raw Data**: 
   Place your raw data files in the `data/raw` directory. Ensure that the files are in a supported format for the data loader.

## Manual Labeling Process

1. **Run the Manual Labeler**: 
   Execute the manual labeling script to start labeling the raw data:
   ```
   python src/manual_labeler.py
   ```
   Follow the on-screen instructions to label the data. The labeled data will be saved in the `labeled_data` directory.

## Training the Model

1. **Train the Model**: 
   Once you have labeled your data, you can train the machine learning model by running:
   ```
   python src/train_model.py
   ```
   This will load the labeled data, prepare it for training, and execute the training process.

## Utility Functions

The `src/utils.py` file contains various utility functions that can be used throughout the project for tasks such as data preprocessing and visualization.


## License

This project is licensed under the MIT License. See the LICENSE file for more details.