import json
import numpy as np
import matplotlib.pyplot as plt


def export_spikes_for_steps_and_sample(spiking_input, output_file, steps_to_export, sample_idx):
    """
    Exports spike data for the specified steps and sample to a file.

    :param spiking_input: Input spike data
    :param output_file: Name of the output file
    :param steps_to_export: List of steps to export (0-based indices)
    :param sample_idx: Index of the sample to export
    """
    spiking_input = np.array(spiking_input)

    num_steps = spiking_input.shape[0]
    num_samples = spiking_input.shape[1]

    # Check if the steps and sample are valid
    if max(steps_to_export) >= num_steps:
        raise ValueError(f"One or more steps are out of bounds (0-{num_steps - 1}).")
    if sample_idx >= num_samples:
        raise ValueError(f"Sample {sample_idx} is out of bounds (0-{num_samples - 1}).")

    with open(output_file, 'w') as f:
        for step_to_export in steps_to_export:
            spike_data = spiking_input[step_to_export, sample_idx]  # Shape: [channels, height, width]
            flat_spikes = spike_data.flatten()
            # f.write(f"# Step {step_to_export}\n")  # Comment for each step
            for spike in flat_spikes:
                f.write(str(int(spike)) + '\n')

    print(f"Data for steps {steps_to_export} and sample {sample_idx} successfully exported to {output_file}")


def plot_spikes_with_average(spiking_input, sample_idx, channel_idx=0):
    """
    Plots the spike image for all time steps, sample, and channel.
    Also displays the average over all steps.

    :param spiking_input: Input spike data
    :param sample_idx: Index of the sample to plot
    :param channel_idx: Index of the channel to plot
    """
    num_steps = spiking_input.shape[0]
    num_samples = spiking_input.shape[1]

    if sample_idx >= num_samples:
        raise ValueError(f"Sample {sample_idx} is out of bounds (0-{num_samples - 1}).")

    plt.figure(figsize=(12, 6))

    # Visualization of spikes at each time step
    for time_step in range(num_steps):
        plt.subplot(2, num_steps + 1, time_step + 1)  # (2, num_steps+1) leave space for the average
        spike_data = spiking_input[time_step, sample_idx, channel_idx]
        plt.imshow(spike_data, cmap='gray')
        plt.title(f"Tick {time_step + 1}")
        plt.axis('off')

    # Visualization of the average over all steps
    avg_spiking = spiking_input[:, sample_idx, channel_idx, :, :].mean(axis=0)
    plt.subplot(2, num_steps + 1, num_steps + 1)  # last space for the average image
    plt.imshow(avg_spiking, cmap='hot')
    plt.title("Average Spikes")
    plt.axis('off')

    plt.show()


# Load spike input data from JSON file
with open("exported_spiking_input.json", 'r') as json_file:
    data = json.load(json_file)

spiking_input = np.array(data["spiking_input"])

# Example usage: export data for steps 0, 1, and 2 for sample 12 to the file 'fifo_data_12_steps.txt'
steps_to_export = [0, 1, 2]  # Specify the steps to export (0-based indices)
sample_idx_to_export = 17  # Specify the sample index to export
export_spikes_for_steps_and_sample(spiking_input, "fifo_data_sample_"+str(sample_idx_to_export)+".txt", steps_to_export, sample_idx_to_export)

# Example usage: visualize spikes and average image for all steps of sample 12, channel 0
plot_spikes_with_average(spiking_input, sample_idx_to_export, channel_idx=0)