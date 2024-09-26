import torch
import json

def tensor_to_list(obj):
    if isinstance(obj, torch.Tensor):
        return obj.tolist()
    elif isinstance(obj, dict):
        return {k: tensor_to_list(v) for k, v in obj.items()}
    elif isinstance(obj, list):
        return [tensor_to_list(i) for i in obj]
    else:
        return obj

def pth_to_json(pth_file, json_file, include_weights=True):
    """
    Converts data from a .pth file to JSON with the option to include or exclude weights.

    :param pth_file: Path to the .pth file
    :param json_file: Path to the file where the JSON result will be saved
    :param include_weights: If True, weights will be added to JSON, otherwise they will be excluded
    """
    data = torch.load(pth_file)

    model_data = {
        'model_topology': tensor_to_list(data.get('model_topology', {})),
        'LIF_neurons': tensor_to_list(data.get('LIF_neurons', {})),
    }

    if include_weights:
        model_data['model_state_dict'] = tensor_to_list(data.get('model_state_dict', {}))

    with open(json_file, 'w') as f:
        json.dump(model_data, f, indent=4)

if __name__ == "__main__":
    pth_to_json('exported_model.pth', 'model_data.json', include_weights=False)