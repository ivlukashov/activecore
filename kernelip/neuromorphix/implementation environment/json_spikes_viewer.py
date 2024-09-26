import json
import numpy as np
import matplotlib.pyplot as plt

def load_spiking_input(file_path):
    """
    Load the spiking input data and labels from a JSON file.
    :param file_path: Path to the JSON file containing the spiking input data and labels.
    :return: Numpy array of the spiking input data and labels.
    """
    with open(file_path, 'r') as json_file:
        data = json.load(json_file)

    # Check the structure of the spiking_input field
    spiking_input = data["spiking_input"]
    labels = np.array(data["labels"])

    # Ensure that spiking_input has a consistent shape
    try:
        spiking_input = np.array(spiking_input)
    except ValueError as e:
        print("Error converting spiking_input to NumPy array:", e)
        print("Inspecting the structure of spiking_input...")
        for i, item in enumerate(spiking_input):
            print(f"Item {i} shape: {np.shape(item)}")
        return None, None

    return spiking_input, labels


def visualize_spiking_input(spiking_input, labels, num_images=5):
    """
    Visualize the spiking input data and associated labels.
    :param spiking_input: Numpy array of the spiking input data.
    :param labels: Numpy array of the labels associated with the spiking input data.
    :param num_images: Number of images to visualize.
    """
    if spiking_input is None:
        print("Spiking input data is not available due to a loading error.")
        return

    num_steps, batch_size, _, height, width = spiking_input.shape

    plt.figure(figsize=(10, 4))
    for i in range(min(num_images, batch_size)):
        # Visualize the first time step of the first image in the batch
        plt.subplot(2, num_images, i + 1)
        plt.imshow(spiking_input[0, i, 0, :, :], cmap='gray')
        plt.title(f"Label: {labels[i]}")
        plt.axis('off')

        # Visualize the average spike activity over all time steps
        avg_spiking = spiking_input[:, i, 0, :, :].mean(axis=0)
        plt.subplot(2, num_images, num_images + i + 1)
        plt.imshow(avg_spiking, cmap='hot')
        plt.title(f"Avg Spikes (Label: {labels[i]})")
        plt.axis('off')

    plt.show()

def visualize_single_input(spiking_input, labels, index):
    """
    Visualize a single spiking input and its corresponding label.
    :param spiking_input: Numpy array of the spiking input data.
    :param labels: Numpy array of the labels associated with the spiking input data.
    :param index: Index of the input to visualize.
    """
    if spiking_input is None:
        print("Spiking input data is not available due to a loading error.")
        return

    num_steps, batch_size, _, height, width = spiking_input.shape

    if index >= batch_size:
        print(f"Index {index} out of bounds for batch size {batch_size}")
        return

    plt.figure(figsize=(8, 4))

    # Visualize the first time step of the selected image
    plt.subplot(1, 2, 1)
    plt.imshow(spiking_input[0, index, 0, :, :], cmap='gray')
    plt.title(f"Label: {labels[index]} (First Time Step)")
    plt.axis('off')

    # Visualize the average spike activity over all time steps for the selected image
    avg_spiking = spiking_input[:, index, 0, :, :].mean(axis=0)
    plt.subplot(1, 2, 2)
    plt.imshow(avg_spiking, cmap='hot')
    plt.title(f"Avg Spikes (Label: {labels[index]})")
    plt.axis('off')

    plt.show()


# Example usage
file_path = 'worked_exported_spiking_input.json'
spiking_input, labels = load_spiking_input(file_path)
visualize_spiking_input(spiking_input, labels)
visualize_single_input(spiking_input, labels, index=0)  # Visualize the spiking input at index 0