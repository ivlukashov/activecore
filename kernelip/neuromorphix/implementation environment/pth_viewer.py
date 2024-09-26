import torch


def load_model_data(data, output_type="full", layer_name=None, num_weights="sample"):
    """
    Display information from the model data depending on the choice of output type, layer, and number of weights.

    :param data: Model data (dictionary from a .pth file).
    :param output_type: Output type ('weights' to display only weights or 'full' to display all information).
    :param layer_name: Name of a specific layer to display its weights. If None, all layers are displayed.
    :param num_weights: Number of weights to display ('sample' for the first 5 weights, 'all' for the entire matrix, or a number).
    """
    if isinstance(data, dict):
        if output_type == 'full':
            # Display model topology
            if 'model_topology' in data:
                print("\n--- Model Topology ---")
                for key, value in data['model_topology'].items():
                    print(f"{key}: {value}")

            # Display LIF neurons parameters
            if 'LIF_neurons' in data:
                print("\n--- LIF Neurons Parameters ---")
                for neuron, params in data['LIF_neurons'].items():
                    print(f"\nNeuron Layer: {neuron}")
                    for param_key, param_value in params.items():
                        print(f"{param_key}: {param_value}")

        # Display model weights (if 'weights' or 'full' is selected)
        if output_type == 'weights' or output_type == 'full':
            if 'model_state_dict' in data:
                print("\n--- Model Weights ---")
                state_dict = data['model_state_dict']

                if layer_name:  # If a specific layer is provided
                    if layer_name in state_dict:
                        print(f"\nLayer: {layer_name}")
                        weights = state_dict[layer_name]
                        print(f"Weights Shape: {weights.shape}")
                        if len(weights.shape) == 2:
                            print(f"Rows: {weights.shape[0]}, Columns: {weights.shape[1]}")

                        if num_weights == "all":
                            print(f"All Weights:\n{weights}")
                        else:
                            try:
                                num_weights = int(num_weights)
                                print(f"Sample Weights (first {num_weights} weights): {weights.view(-1)[:num_weights]}")
                            except ValueError:
                                print(f"Sample Weights (first 5 weights): {weights.view(-1)[:5]}")
                    else:
                        print(f"Layer '{layer_name}' not found in model.")
                else:
                    # Display all layers
                    for layer_name, weights in state_dict.items():
                        print(f"\nLayer: {layer_name}")
                        print(f"Weights Shape: {weights.shape}")
                        if len(weights.shape) == 2:
                            print(f"Rows: {weights.shape[0]}, Columns: {weights.shape[1]}")

                        if num_weights == "all":
                            print(f"All Weights:\n{weights}")
                        else:
                            try:
                                num_weights = int(num_weights)
                                print(f"Sample Weights (first {num_weights} weights): {weights.view(-1)[:num_weights]}")
                            except ValueError:
                                print(f"Sample Weights (first 5 weights): {weights.view(-1)[:5]}")
        else:
            print("Invalid output parameter. Please enter 'weights' or 'full'.")
    else:
        print("Data is not in the expected format.")


if __name__ == "__main__":
    # Set parameters for data display
    output_type = "weights"  # Can specify 'weights' to display only weights or 'full' for all information
    layer_to_print = "fc1.weight"  # Specify a specific layer or None to display all layers
    num_weights = "all"  # Specify 'all' to display all weights or a number to display a specific amount

    # Load data from .pth file
    data = torch.load("exported_model.pth")

    # Call the function to display data
    load_model_data(data, output_type, layer_name=layer_to_print, num_weights=num_weights)