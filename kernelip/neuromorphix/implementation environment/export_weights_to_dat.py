import torch


# Function to convert a floating-point number to fixed-point and hexadecimal format
def float_to_fixed_point_hex(value, int_bits, frac_bits):
    # Calculate the scaling factor for the fractional part
    scaling_factor = 2 ** frac_bits

    # Convert the number to an integer value
    fixed_point_value = int(round(value * scaling_factor))

    # Calculate the total number of bits to store the number
    total_bits = int_bits + frac_bits

    # Check if the number fits in the specified number of bits
    max_value = 2 ** (total_bits - 1) - 1
    min_value = -2 ** (total_bits - 1)
    if fixed_point_value > max_value or fixed_point_value < min_value:
        raise ValueError(
            f"The number {value} does not fit in the format with {int_bits} integer bits and {frac_bits} fractional bits.")

    # Convert to two's complement format
    if fixed_point_value < 0:
        fixed_point_value = (1 << total_bits) + fixed_point_value

    # Convert to hexadecimal representation
    hex_value = format(fixed_point_value, '04X')

    return hex_value


# Function to export layer weights to weights.dat file
def export_weights_to_dat(layer_name, pth_file="exported_model.pth", output_file="weights_fc2.dat", integer_bits=2,
                          fractional_bits=14):
    # Load the .pth file
    data = torch.load(pth_file)

    # Extract the state_dict
    state_dict = data['model_state_dict']

    # Check if the layer exists
    if layer_name not in state_dict:
        raise ValueError(f"Layer '{layer_name}' not found in the model's state_dict.")

    # Extract weights for the specified layer
    layer_weight = state_dict[layer_name]  # Example: 'fc1.weight' or 'fc2.weight'

    # Debug information: shape of the weights
    print(f"Exporting weights from layer '{layer_name}' with shape: {layer_weight.shape}")

    # Write weights to file in hexadecimal format, suitable for $readmemh
    with open(output_file, 'w') as f:
        for postsyn_idx in range(layer_weight.shape[0]):
            for presyn_idx in range(layer_weight.shape[1]):
                value = layer_weight[postsyn_idx][presyn_idx].item()
                hex_value = float_to_fixed_point_hex(value, integer_bits, fractional_bits)
                f.write(hex_value + '\n')

    # Debug information: examples of transformed weights
    print(f"\nExamples of weight values and their conversion to fixed-point from layer '{layer_name}':")

    # Output the first 5 weight values
    for i in range(min(5, layer_weight.numel())):
        original_value = round(layer_weight.view(-1)[i].item(), 4)  # Round the original value to 4 decimal places
        hex_value = float_to_fixed_point_hex(original_value, integer_bits, fractional_bits)
        # Output in scientific notation format, as in PyTorch
        scientific_value = "{:.4e}".format(layer_weight.view(-1)[i].item())
        print(f"First values - Original: {original_value:.4f}, Hex: {hex_value}, Scientific: {scientific_value}")

    print("\n...\n")

    # Output the last 5 weight values
    for i in range(-5, 0):
        original_value = round(layer_weight.view(-1)[i].item(), 4)  # Round the original value to 4 decimal places
        hex_value = float_to_fixed_point_hex(original_value, integer_bits, fractional_bits)
        # Output in scientific notation format, as in PyTorch
        scientific_value = "{:.4e}".format(layer_weight.view(-1)[i].item())
        print(f"Last values - Original: {original_value:.4f}, Hex: {hex_value}, Scientific: {scientific_value}")

    print(f"\nWeights successfully exported to {output_file}")


# Example usage
# Choose the layer and the number of bits for the integer and fractional part
export_weights_to_dat('fc2.weight', integer_bits=2, fractional_bits=14)