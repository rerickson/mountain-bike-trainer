# rolling_average_filter.py
import pandas as pd

def apply_rolling_average(data, window=10):
    """
    Applies a rolling average filter to the given data.

    Args:
        data (list of dict): A list of dictionaries, where each dictionary
                             represents a data point with numeric values.
        window (int): The size of the rolling window.

    Returns:
        list of dict: A list of dictionaries with rolling average applied to numeric fields.
    """
    if not data:
        return []

    # Convert list of dicts to DataFrame
    df = pd.DataFrame(data)

    # Identify numeric columns
    numeric_cols = df.select_dtypes(include=['number']).columns

    # Apply rolling average to numeric columns
    df[numeric_cols] = df[numeric_cols].rolling(window=window,center=True, min_periods=1).mean()

    # Convert back to list of dicts
    filtered_data = df.to_dict(orient='records')

    return filtered_data