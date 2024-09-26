import torch
import torchvision
import torchvision.transforms as transforms
import matplotlib.pyplot as plt
import numpy as np
from scipy.ndimage import zoom

# Image size
x_size = 28
y_size = 28

# Threshold for binarization
threshold = 20
num_steps = 5  # Number of time steps for spiking activity

# Function to resize the image
def resize_image(image, target_size=(x_size, y_size)):
    return zoom(image, (target_size[0] / image.shape[0], target_size[1] / image.shape[1]))

# Function to apply thresholding
def soft_thresholding(image, threshold):
    return np.where(image > threshold, 1, 0)

# Function for spike encoding
def rate_encoding(x, num_steps):
    x = (x + 1) / 2  # Normalize values between 0 and 1
    return torch.bernoulli(x.repeat(num_steps, 1, 1, 1, 1))  # Generate spikes

# Loading the MNIST dataset
mnist_test = torchvision.datasets.MNIST(root='./data', train=False, download=True)
test_images = mnist_test.data.numpy()

# Select one image for example (e.g., the 0th image)
image_idx = 0
original_image = test_images[image_idx]

# Preprocessing the image
preprocessed_image = resize_image(original_image)
preprocessed_image = soft_thresholding(preprocessed_image, threshold)

# Convert the preprocessed image to a tensor
image_tensor = torch.tensor(preprocessed_image, dtype=torch.float32).unsqueeze(0).unsqueeze(0)  # Add batch and channel dimensions

# Generate spikes for several time steps
spiking_input = rate_encoding(image_tensor, num_steps)

# Visualize the results
fig, axes = plt.subplots(1, num_steps + 3, figsize=(15, 5))

# Original image
axes[0].imshow(original_image, cmap='gray')
axes[0].set_title('Original')
axes[0].axis('off')

# Preprocessed image
axes[1].imshow(preprocessed_image, cmap='gray')
axes[1].set_title('Preprocessed')
axes[1].axis('off')

# Spikes at each time step
for time_step in range(num_steps):
    spike_data = spiking_input[time_step, 0, 0]  # Select spikes for the first channel and batch
    axes[time_step + 2].imshow(spike_data.cpu().numpy(), cmap='gray')
    axes[time_step + 2].set_title(f'Tick {time_step + 1}')
    axes[time_step + 2].axis('off')

# Averaged spikes
avg_spiking = spiking_input.mean(dim=0)[0, 0]  # Average over time steps
axes[-1].imshow(avg_spiking.cpu().numpy(), cmap='hot')
axes[-1].set_title('Average Spikes')
axes[-1].axis('off')

plt.tight_layout()
plt.show()