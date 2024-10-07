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
        raise ValueError(f"The number {value} does not fit in the format with {int_bits} integer bits and {frac_bits} fractional bits.")

    # Convert to two's complement format
    if fixed_point_value < 0:
        fixed_point_value = (1 << total_bits) + fixed_point_value

    # Convert to hexadecimal representation
    hex_value = hex(fixed_point_value)

    return hex_value

# Example usage:
value = -0.0014
int_bits = 3  # number of bits for the integer part
frac_bits = 13  # number of bits for the fractional part

hex_representation = float_to_fixed_point_hex(value, int_bits, frac_bits)
print(f"Hex representation of the number {value} in the format with {int_bits} integer bits and {frac_bits} fractional bits: {hex_representation}")