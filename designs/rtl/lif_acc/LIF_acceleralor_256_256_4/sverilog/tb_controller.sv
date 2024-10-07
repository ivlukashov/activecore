`timescale 1ns/1ps

module Neuromorphic_design_tb;

    localparam CLK_PERIOD = 10;

    reg clk;
    reg rst;
    reg start_processing;

    // Переменные для преобразованных значений
    real membr_pot_mem_float_0;
    real membr_pot_mem_float_1;
    real membr_pot_mem_float_2;
    real membr_pot_mem_float_3;
    real membr_pot_mem_float_4;
    real membr_pot_mem_float_5;
    real membr_pot_mem_float_6;
    real membr_pot_mem_float_7;
    real membr_pot_mem_float_8;
    real membr_pot_mem_float_9;

    real weights_mem_float_0;
    real weights_mem_float_1;
    real weights_mem_float_2;
    real weights_mem_float_3;
    real weights_mem_float_4;
    real weights_mem_float_5;
    real weights_mem_float_6;
    real weights_mem_float_7;
    real weights_mem_float_8;
    real weights_mem_float_9;


    Neuromorphic_design dut (
        .clk_i(clk),
        .rst_i(rst),
        .start_processing(start_processing)
    );

    // Функция для преобразования фиксированной точки в плавающую
    function real fixed_to_float(input signed [15:0] fixed_value);
        begin
//            fixed_to_float = fixed_value / 256.0;  // Преобразуем число с 8-битной дробной частью
//            fixed_to_float = fixed_value / 8192.0;  // Преобразуем число с 8-битной дробной частью
            fixed_to_float = fixed_value / 16384.0;

        end
    endfunction

    // Преобразование и обновление переменных для мониторинга
    always @(posedge clk) begin
        membr_pot_mem_float_0 = fixed_to_float(dut.membrane_potential_memory[0]);
        membr_pot_mem_float_1 = fixed_to_float(dut.membrane_potential_memory[1]);
        membr_pot_mem_float_2 = fixed_to_float(dut.membrane_potential_memory[2]);
        membr_pot_mem_float_3 = fixed_to_float(dut.membrane_potential_memory[3]);
        membr_pot_mem_float_4 = fixed_to_float(dut.membrane_potential_memory[4]);
        membr_pot_mem_float_5 = fixed_to_float(dut.membrane_potential_memory[5]);
        membr_pot_mem_float_6 = fixed_to_float(dut.membrane_potential_memory[6]);
        membr_pot_mem_float_7 = fixed_to_float(dut.membrane_potential_memory[7]);
        membr_pot_mem_float_8 = fixed_to_float(dut.membrane_potential_memory[8]);
        membr_pot_mem_float_9 = fixed_to_float(dut.membrane_potential_memory[9]);
    end

    always @(posedge clk) begin
        weights_mem_float_0 = fixed_to_float(dut.weights_mem[0][0]);
        weights_mem_float_1 = fixed_to_float(dut.weights_mem[0][1]);
        weights_mem_float_2 = fixed_to_float(dut.weights_mem[0][2]);
        weights_mem_float_3 = fixed_to_float(dut.weights_mem[0][3]);
        weights_mem_float_4 = fixed_to_float(dut.weights_mem[0][4]);
        weights_mem_float_5 = fixed_to_float(dut.weights_mem[0][5]);
        weights_mem_float_6 = fixed_to_float(dut.weights_mem[0][6]);
        weights_mem_float_7 = fixed_to_float(dut.weights_mem[0][7]);
        weights_mem_float_8 = fixed_to_float(dut.weights_mem[0][8]);
        weights_mem_float_9 = fixed_to_float(dut.weights_mem[0][9]);
    end

    always #5 clk = ~clk;

    initial begin
        $readmemh("weights.dat", dut.weights_mem);
        $readmemb("fifo_data.txt", dut.in_fifo);

        @(negedge rst);
    end


    initial begin
        clk = 0;
        rst = 1;
        start_processing = 0;

        $monitor("Time=%0t | curr_state=%0d | in_spk_num=%0d, membr[0]=%.14f, membr[1]=%.14f, membr[2]=%.14f, membr[3]=%.14f, membr[4]=%.14f, membr[5]=%.14f, membr[6]=%.14f, membr[7]=%.14f, membr[8]=%.14f, membr[9]=%.14f |w_presyn_idx=%d, w_postsyn_idx=%d | presyn_num=%d | postsyn_num=%d, w0=%.8f, w1=%.8f, w2=%.8f",
            $time, dut.current_state, dut.input_spike_num, membr_pot_mem_float_0, membr_pot_mem_float_1, membr_pot_mem_float_2, membr_pot_mem_float_3, membr_pot_mem_float_4, membr_pot_mem_float_5, membr_pot_mem_float_6, membr_pot_mem_float_7, membr_pot_mem_float_8, membr_pot_mem_float_9,  dut.weight_presyn_idx, dut.weight_postsyn_idx, dut.presynapse_neuron_number, dut.postsynapse_neuron_number, weights_mem_float_0, weights_mem_float_1, weights_mem_float_2); // Мониторим состояние in_fifo


//        $monitor("Time=%0t | curr_state=%0d | in_spk_num=%0d, membr_pot_mem[0]=%.8f, membr_pot_mem[1]=%.8f, membr_pot_mem[2]=%.8f, membr_pot_mem[3]=%.8f, membr_pot_mem[4]=%.8f, membr_pot_mem[5]=%.8f, membr_pot_mem[6]=%.8f, membr_pot_mem[7]=%.8f, membr_pot_mem[8]=%.8f, membr_pot_mem[9]=%.8f, weight_presyn_idx=%0d | weight_postsyn_idx=%0d | presyn_num=%0d | postsyn_num=%0d | weight_upd=%0d, out=%0d%0d%0d%0d%0d%0d%0d%0d%0d%0d, in=%0d, %0d",
//            $time, dut.current_state, dut.input_spike_num,
//            membr_pot_mem_float_0, membr_pot_mem_float_1, membr_pot_mem_float_2, membr_pot_mem_float_3, membr_pot_mem_float_4,
//            membr_pot_mem_float_5, membr_pot_mem_float_6, membr_pot_mem_float_7, membr_pot_mem_float_8, membr_pot_mem_float_9,
//            dut.weight_presyn_idx, dut.weight_postsyn_idx, dut.presynapse_neuron_number, dut.postsynapse_neuron_number,
//            dut.weight_upd, dut.out_fifo[0], dut.out_fifo[1], dut.out_fifo[2], dut.out_fifo[3], dut.out_fifo[4], dut.out_fifo[5], dut.out_fifo[6], dut.out_fifo[7], dut.out_fifo[8], dut.out_fifo[9], dut.in_fifo[0], dut.in_fifo[4]); // Мониторим состояние in_fifo

//        $monitor("Time=%0t | w0=%0d | w1=%0d, w2=%.8f, w3=%.8f, w4=%.8f, w5=%.8f, w6=%.8f, w7=%.8f, w8=%.8f,  w9=%.8f",
//            $time,  weights_mem_float_0, weights_mem_float_1, weights_mem_float_2, weights_mem_float_3, weights_mem_float_4, weights_mem_float_5, weights_mem_float_6, weights_mem_float_7, weights_mem_float_8, weights_mem_float_9); // Мониторим состояние in_fifo


        #10 rst = 0;
        #20 start_processing = 1;
        #150 start_processing = 0;
        #1000000;
        $finish;
    end

    initial begin
        $dumpfile("Neuromorphic_design.vcd");
        $dumpvars(0, Neuromorphic_design_tb);
        $dumpvars(0, dut.presynapse_neuron_number);
    end

endmodule : Neuromorphic_design_tb