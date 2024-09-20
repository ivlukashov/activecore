`timescale 1ns/1ps

module Neuromorphic_design_tb;

    localparam CLK_PERIOD = 10;

    reg clk;
    reg rst;
    reg start_processing;
    logic dbg_sig_rd_fifo;
    logic fifo_input_genfifo_req_i;
    logic [0:0] fifo_input_genfifo_rdata_bi [15:0];
    logic fifo_input_genfifo_ack_o;


    Neuromorphic_design dut (
        .clk_i(clk),
        .rst_i(rst),
        .start_processing(start_processing),
        .dbg_sig_rd_fifo(dbg_sig_rd_fifo),
        .fifo_input_genfifo_req_i(fifo_input_genfifo_req_i),
        .fifo_input_genfifo_rdata_bi(fifo_input_genfifo_rdata_bi),
        .fifo_input_genfifo_ack_o(fifo_input_genfifo_ack_o)
    );

    logic [0:0] fifo_data [15:0];

    always #5 clk = ~clk;

    initial begin
        fifo_input_genfifo_req_i = 0;
        fifo_input_genfifo_rdata_bi[0] = 0;
        fifo_input_genfifo_rdata_bi[1] = 0;
        fifo_input_genfifo_rdata_bi[2] = 0;
        fifo_input_genfifo_rdata_bi[3] = 0;
        fifo_input_genfifo_rdata_bi[4] = 0;
        fifo_input_genfifo_rdata_bi[5] = 0;
        fifo_input_genfifo_rdata_bi[6] = 0;
        fifo_input_genfifo_rdata_bi[7] = 0;
        fifo_input_genfifo_rdata_bi[8] = 0;
        fifo_input_genfifo_rdata_bi[9] = 0;
        fifo_input_genfifo_rdata_bi[10] = 0;
        fifo_input_genfifo_rdata_bi[11] = 0;
        fifo_input_genfifo_rdata_bi[12] = 0;
        fifo_input_genfifo_rdata_bi[13] = 0;
        fifo_input_genfifo_rdata_bi[14] = 0;
        fifo_input_genfifo_rdata_bi[15] = 0;

        dbg_sig_rd_fifo = 0;

        $readmemb("fifo_data.txt", fifo_data);

        @(negedge rst);

        # (CLK_PERIOD * 2);
        dbg_sig_rd_fifo = 1;

        write_all_fifo();

        # (CLK_PERIOD * 10);
        dbg_sig_rd_fifo = 0;
    end

    task write_all_fifo();
        begin
            fifo_input_genfifo_rdata_bi[0] = fifo_data[0];
            fifo_input_genfifo_rdata_bi[1] = fifo_data[1];
            fifo_input_genfifo_rdata_bi[2] = fifo_data[2];
            fifo_input_genfifo_rdata_bi[3] = fifo_data[3];
            fifo_input_genfifo_rdata_bi[4] = fifo_data[4];
            fifo_input_genfifo_rdata_bi[5] = fifo_data[5];
            fifo_input_genfifo_rdata_bi[6] = fifo_data[6];
            fifo_input_genfifo_rdata_bi[7] = fifo_data[7];
            fifo_input_genfifo_rdata_bi[8] = fifo_data[8];
            fifo_input_genfifo_rdata_bi[9] = fifo_data[9];
            fifo_input_genfifo_rdata_bi[10] = fifo_data[10];
            fifo_input_genfifo_rdata_bi[11] = fifo_data[11];
            fifo_input_genfifo_rdata_bi[12] = fifo_data[12];
            fifo_input_genfifo_rdata_bi[13] = fifo_data[13];
            fifo_input_genfifo_rdata_bi[14] = fifo_data[14];
            fifo_input_genfifo_rdata_bi[15] = fifo_data[15];



            fifo_input_genfifo_req_i = 1;  // Поднимаем сигнал запроса
            @(posedge clk);  // Ждем позитивного фронта такта
            wait (fifo_input_genfifo_ack_o == 1);  // Ждем подтверждения записи
            fifo_input_genfifo_req_i = 0;  // Сбрасываем сигнал запроса
            @(posedge clk);  // Переходим на следующий такт
        end
    endtask

    initial begin
        $readmemh("weights.dat", dut.weights_mem);
        @(negedge rst);
    end

    initial begin
        clk = 0;
        rst = 1;
        start_processing = 0;

//        $monitor("Time=%0t | presynapse_neuron_number=%0d | postsynapse_neuron_number=%0d | reg_start_processing=%d | current_state=%d | actual_spike=%d | in_fifo[0]=%b | in_fifo[1]=%b | in_fifo[2]=%b | in_fifo[3]=%b | input_spike_num=%d",
//            $time, dut.presynapse_neuron_number, dut.postsynapse_neuron_number, dut.reg_start_processing, dut.current_state, dut.actual_spike,
//            dut.in_fifo[0], dut.in_fifo[1], dut.in_fifo[2], dut.in_fifo[3], dut.input_spike_num); // Мониторим состояние in_fifo

//        $monitor("Time=%0t | presynapse_neuron_number=%0d | postsynapse_neuron_number=%0d | reg_start_processing=%d | current_state=%d | actual_spike=%d | input_spike_num=%d, weights_mem[0][0]=%d, dut.weights_mem[0][1]=%d, dut.weights_mem[1][0]=%d, dut.weights_mem[1][1]=%d, membrane_potential_memory[0]=%d, membrane_potential_memory[1]=%d, membrane_potential_memory[2]=%d",
//            $time, dut.presynapse_neuron_number, dut.postsynapse_neuron_number, dut.reg_start_processing, dut.current_state, dut.actual_spike,
//            dut.input_spike_num, dut.weights_mem[0][0], dut.weights_mem[0][1], dut.weights_mem[0][2], dut.weights_mem[1][1],
//            dut.membrane_potential_memory[0], dut.membrane_potential_memory[1], dut.membrane_potential_memory[2]); // Мониторим состояние in_fifo

//        $monitor("Time=%0t | presyn_num=%0d | postsyn_num=%0d | start_proc=%0d | curr_state=%0d | actual_spk=%0d | in_spk_num=%0d, membr_pot_mem[0]=%0d, membr_pot_mem[1]=%0d, membr_pot_mem[2]=%0d, out_fifo[0]=%0d, out_fifo[1]=%0d, out_fifo[2]=%0d, w00=%0d, w01=%0d, w02=%0d, w10=%0d, w11=%0d, w12=%0d, w20=%0d, w21=%0d, w22=%0d,  ",
//            $time, dut.presynapse_neuron_number, dut.postsynapse_neuron_number, dut.reg_start_processing, dut.current_state, dut.actual_spike,
//            dut.input_spike_num, dut.membrane_potential_memory[0], dut.membrane_potential_memory[1], dut.membrane_potential_memory[2], dut.out_fifo[0], dut.out_fifo[1], dut.out_fifo[2], dut.weights_mem[0][0], dut.weights_mem[0][1], dut.weights_mem[0][2], dut.weights_mem[1][0], dut.weights_mem[1][1], dut.weights_mem[1][2], dut.weights_mem[2][0], dut.weights_mem[2][1], dut.weights_mem[2][2],); // Мониторим состояние in_fifo

        $monitor("Time=%0t | curr_state=%0d | in_spk_num=%0d, membr_pot_mem[0]=%0d, membr_pot_mem[1]=%0d, weight_presyn_idx=%0d | weight_postsyn_idx=%0d | presyn_num=%0d | postsyn_num=%0d | weight_upd=%0d, out=%0d%0d%0d%0d%0d%0d%0d%0d%0d%0d",
            $time, dut.current_state, dut.input_spike_num, dut.membrane_potential_memory[0], dut.membrane_potential_memory[1], dut.weight_presyn_idx, dut.weight_postsyn_idx, dut.presynapse_neuron_number, dut.postsynapse_neuron_number, dut.weight_upd, dut.out_fifo[0], dut.out_fifo[1], dut.out_fifo[2], dut.out_fifo[3], dut.out_fifo[4], dut.out_fifo[5], dut.out_fifo[6], dut.out_fifo[7], dut.out_fifo[8], dut.out_fifo[9]); // Мониторим состояние in_fifo



        #10 rst = 0;

        #20 start_processing = 1;
        #150 start_processing = 0;

        #1000;
        $finish;
    end

    initial begin
        $dumpfile("Neuromorphic_design.vcd");
        $dumpvars(0, Neuromorphic_design_tb);
        $dumpvars(0, dut.presynapse_neuron_number);
    end

endmodule : Neuromorphic_design_tb