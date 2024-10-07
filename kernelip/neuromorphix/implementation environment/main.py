import snntorch as snn
import torch.nn as nn
import torchvision
import torchvision.transforms as transforms
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset
from scipy.ndimage import zoom
import numpy as np
import torch
import json
import os
import matplotlib.pyplot as plt


# Device configuration
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# Define image size
x_size = 28
y_size = 28

# Hyperparameters
num_inputs = x_size * y_size  # For MNIST
num_hidden = 10
num_outputs = 10
beta = 0.5  # Decay rate for Leaky Integrate-and-Fire (LIF) neurons
num_steps = 5  # Simulation time steps
batch_size = 28 * 28  # 256
learning_rate = 1e-3
num_epochs = 10

# Function to resize the image to the desired size
def resize_image(image, target_size=(x_size, y_size)):
    return zoom(image, (target_size[0] / image.shape[0], target_size[1] / image.shape[1]))

# Function to apply soft thresholding
def soft_thresholding(image, threshold):
    return np.where(image > threshold, 1, 0)

# Apply preprocessing to all images
def preprocess_images(images, preprocess_fns, limit=None):
    preprocessed_images = []
    original_images = []
    for i, image in enumerate(images):
        original_image = image  # Keep the original for visualization
        for fn in preprocess_fns:
            image = fn(image)
        preprocessed_images.append(image)
        original_images.append(original_image)
        if limit is not None and i + 1 >= limit:  # Limit to specified number of images
            break
    return np.array(preprocessed_images), np.array(original_images)

# Load and preprocess MNIST dataset
def load_and_preprocess_mnist(threshold=20, target_size=(x_size, y_size), visualize_count=5):
    mnist_train = torchvision.datasets.MNIST(root='./data', train=True, download=True)
    mnist_test = torchvision.datasets.MNIST(root='./data', train=False, download=True)

    train_images = mnist_train.data.numpy()
    train_labels = mnist_train.targets.numpy()
    test_images = mnist_test.data.numpy()
    test_labels = mnist_test.targets.numpy()

    # Preprocess images for visualization (limited number)
    train_images_processed_viz, original_images = preprocess_images(
        train_images, [lambda x: resize_image(x, target_size), lambda x: soft_thresholding(x, threshold)],
        limit=visualize_count)

    # Display first 5 original and processed images
    display_images(original_images, train_images_processed_viz)

    # Process all images
    train_images_processed = preprocess_images(train_images, [lambda x: resize_image(x, target_size),
                                                              lambda x: soft_thresholding(x, threshold)])[0]
    test_images_processed = \
    preprocess_images(test_images, [lambda x: resize_image(x, target_size), lambda x: soft_thresholding(x, threshold)])[0]

    # Convert processed images back to torch tensors
    train_images_tensor = torch.tensor(train_images_processed, dtype=torch.int8).unsqueeze(1)
    test_images_tensor = torch.tensor(test_images_processed, dtype=torch.int8).unsqueeze(1)
    train_labels_tensor = torch.tensor(train_labels, dtype=torch.long)
    test_labels_tensor = torch.tensor(test_labels, dtype=torch.long)

    return (train_images_tensor, train_labels_tensor), (test_images_tensor, test_labels_tensor)

# Function to display first 5 original and processed images
def display_images(originals, processed):
    plt.figure(figsize=(10, 4))
    for i in range(5):
        # Original Image
        plt.subplot(2, 5, i + 1)
        plt.title(f"Original {i + 1}")
        plt.imshow(originals[i], cmap='gray')
        plt.axis('off')

        # Processed Image
        plt.subplot(2, 5, i + 6)
        plt.title(f"Processed {i + 1}")
        plt.imshow(processed[i], cmap='gray')
        plt.axis('off')

    plt.show()

# Load the preprocessed data
(train_images_processed, train_labels), (test_images_processed, test_labels) = load_and_preprocess_mnist()

# Create DataLoader
train_dataset = TensorDataset(train_images_processed, train_labels)
test_dataset = TensorDataset(test_images_processed, test_labels)

# Data Preprocessing and Loading
transform = transforms.Compose([
    transforms.ToTensor(),
    transforms.Normalize((0.5,), (0.5,))
])

train_loader = torch.utils.data.DataLoader(dataset=train_dataset, batch_size=batch_size, shuffle=True)
test_loader = torch.utils.data.DataLoader(dataset=test_dataset, batch_size=batch_size, shuffle=False)

def rate_encoding(x, num_steps):
    # Ensure x is of shape [batch_size, 1, 28, 28]
    if x.dim() == 3:
        x = x.unsqueeze(1)

    # Adjust the input values to be within [0, 1]
    x = (x + 1) / 2

    # Repeat and sample spikes
    return torch.bernoulli(x.repeat(num_steps, 1, 1, 1, 1))

def export_spiking_input(spiking_input, labels, file_path):
    """
    Exports the spiking_input tensor and labels to a JSON file.

    :param spiking_input: Spiking tensor fed into the model, dimensions [num_steps, batch_size, num_inputs].
    :param labels: Tensor of labels for each example in the batch.
    :param file_path: Path where the JSON file will be saved.
    """
    # Convert spiking tensor to a list for JSON serialization
    spiking_input_list = spiking_input.cpu().numpy().tolist()

    # Convert labels to a list
    labels_list = labels.cpu().numpy().tolist()

    # Create data for export
    export_data = {
        "spiking_input": spiking_input_list,
        "labels": labels_list
    }

    # Create directory if it doesn't exist
    os.makedirs(os.path.dirname(file_path), exist_ok=True)

    # Save data to JSON file
    with open(file_path, 'w') as json_file:
        json.dump(export_data, json_file)

    print(f"Spiking input and labels successfully exported to {file_path}")

# Define the Network
class SNN(nn.Module):
    def __init__(self):
        super(SNN, self).__init__()

        # Define fully connected layers
        self.fc1 = nn.Linear(num_inputs, num_hidden, bias=False)
        self.lif1 = snn.Leaky(beta=beta, graded_spikes_factor=1)
        self.fc2 = nn.Linear(num_hidden, num_outputs, bias=False)
        self.lif2 = snn.Leaky(beta=beta, graded_spikes_factor=1)

    def forward(self, x):
        # Initialize hidden states
        mem1 = self.lif1.init_leaky()
        mem2 = self.lif2.init_leaky()

        spk1_rec = []
        spk2_rec = []

        # Time loop
        for step in range(num_steps):
            cur1 = self.fc1(x[step].flatten(1))
            spk1, mem1 = self.lif1(cur1, mem1)
            cur2 = self.fc2(spk1)
            spk2, mem2 = self.lif2(cur2, mem2)

            spk1_rec.append(spk1)
            spk2_rec.append(spk2)

        return torch.stack(spk2_rec), torch.stack(spk1_rec)

    def export_model(self, file_path):
        # Export model weights
        model_state = {
            "model_state_dict": self.state_dict(),
            "model_topology": {
                "input_size": num_inputs,
                "hidden_size": num_hidden,
                "output_size": num_outputs,
                "num_steps": num_steps,
                "beta": beta
            },
            "LIF_neurons": {
                "lif1": {
                    "beta": self.lif1.beta,
                    "threshold": self.lif1.threshold
                },
                "lif2": {
                    "beta": self.lif2.beta,
                    "threshold": self.lif2.threshold
                }
            }
        }

        # Create directory if it doesn't exist
        os.makedirs(os.path.dirname(file_path), exist_ok=True)

        # Save model state
        torch.save(model_state, file_path)

        print(f"Model successfully exported to {file_path}")

# Initialize the network
net = SNN().to(device)

# Loss and optimizer
criterion = nn.CrossEntropyLoss()
optimizer = optim.Adam(net.parameters(), lr=learning_rate)

def print_input_structure(spiking_input):
    """
    Function prints the structure and data of the input tensor for the SNN.
    :param spiking_input: Spiking tensor fed into the model, dimensions [num_steps, batch_size, num_inputs].
    """
    # Print the shape of the input tensor
    print(f"Input Tensor Shape: {spiking_input.shape}")

    # Average the tensor over time steps
    averaged_input = spiking_input.mean(dim=0)  # Dimension after averaging [batch_size, 1, height, width]

    # Print first spikes for the first image in the batch
    print("First Image Spikes (First Few Time Steps):")
    print(spiking_input[:, 0, :, :, :])  # Print all time steps for the first image in the batch

    # Visualize the first image of the first time step
    plt.figure(figsize=(10, 4))

    plt.subplot(1, 2, 1)
    plt.imshow(spiking_input[0, 0, 0, :, :].cpu().numpy(), cmap='gray')
    plt.title("First Time Step")

    plt.show()

    # Print the averaged tensor for the first image in the batch
    print("Averaged Image Spikes:")
    print(averaged_input[0, :, :, :])  # Print averaged image for the first image in the batch

# Training Loop
for epoch in range(num_epochs):
    for i, (images, labels) in enumerate(train_loader):
        images = images.to(device)
        labels = labels.to(device)

        # Rate Encoding
        spiking_input = rate_encoding(images, num_steps).to(device)

        # Forward pass
        outputs, _ = net(spiking_input)

        # Use the mean output over time for classification
        outputs = outputs.mean(dim=0)

        # Compute loss
        loss = criterion(outputs, labels)

        # Backpropagation and optimization
        optimizer.zero_grad()
        loss.backward()
        optimizer.step()

        if (i + 1) % 100 == 0:
            print(f'Epoch [{epoch + 1}/{num_epochs}], Step [{i + 1}/{len(train_loader)}], Loss: {loss.item():.4f}')

# Testing Loop
net.eval()
export_file_path = "exported_spiking_input.json"
with torch.no_grad():
    correct = 0
    total = 0
    for images, labels in test_loader:
        images = images.to(device)
        labels = labels.to(device)

        spiking_input = rate_encoding(images, num_steps).to(device)
        export_spiking_input(spiking_input, labels, "./exported_spiking_input.json")

        outputs, _ = net(spiking_input)
        outputs = outputs.mean(dim=0)

        _, predicted = torch.max(outputs.data, 1)
        total += labels.size(0)
        correct += (predicted == labels).sum().item()

    print(f'Accuracy of the network on the 10000 test images: {100 * correct / total:.2f}%')

net.export_model("./exported_model.pth")