`timescale 1ms/1ns

module Neuromorphic_design_tb;

    // Signals to connect to the counter module
    reg clk_i;
    reg rst_i;
//    logic [1:0] dbg_out_wr_ptr;
    reg input_buf_wr_en;
    logic input_buf_data_in;
    logic [1:0] input_buf_read_addr;
    logic input_buf_rd_en;
    logic [0:0] input_buf_data_out;

    logic dbg_tick;
    logic [31:0]   dbg_clk_counter;
    logic [31:0] dbg_tick_period;
    logic [31:0] dbg_presyn_neuron_counter_num;
    logic [31:0] dbg_postsyn_neuron_counter_num;

    logic [1:0] weight_memory_data_in;
    logic [2:0] cntl_sig;
    logic weight_memory_wr_en;
    logic weight_memory_rd_en;
    logic [1:0] weight_memory_data_out;
//    logic [1:0] weight_memory_data_in;

//    logic dbg_reg_input_buf_data_out;
//    logic dbg_reg_input_buf_rd_en;

    // wire [4:0] count;

    logic weight_val;

    // Instantiate the counter module
    Neuromorphic_design uut (
        .clk_i(clk_i)
        , .rst_i(rst_i)
//        , .dbg_out_wr_ptr(dbg_out_wr_ptr)
        , .input_buf_wr_en(input_buf_wr_en)
        , .input_buf_data_in(input_buf_data_in)
        , .input_buf_read_addr(input_buf_read_addr)
        , .input_buf_rd_en(input_buf_rd_en)
        , .input_buf_data_out(input_buf_data_out)
        , .dbg_tick(dbg_tick)
        , .dbg_clk_counter(dbg_clk_counter)
        , .dbg_tick_period(dbg_tick_period)
        , .dbg_presyn_neuron_counter_num(dbg_presyn_neuron_counter_num)
        , .dbg_postsyn_neuron_counter_num(dbg_postsyn_neuron_counter_num)
        , .weight_memory_data_in(weight_memory_data_in)

        , .cntl_sig(cntl_sig)
        , .weight_memory_wr_en(weight_memory_wr_en)
        , .weight_memory_rd_en(weight_memory_rd_en)

        , .weight_memory_data_out(weight_memory_data_out)
//        , .weight_memory_data_in(weight_memory_data_in)
//    , .dbg_reg_input_buf_data_out(dbg_reg_input_buf_data_out)
//    , .dbg_reg_input_buf_rd_en(dbg_reg_input_buf_rd_en)
        // .count(count)
    );

    // Clock generation
    initial begin
        clk_i = 0;
        forever #5 clk_i = ~clk_i;  // 100 MHz clock (10 ns period)
    end

    // Stimulus generation
    initial begin
        // Open a VCD file for GTKWave
        $dumpfile("Neuromorphic_design.vcd");
        $dumpvars(0, Neuromorphic_design_tb);

        // Initialize signals
        rst_i = 1;

        #10 rst_i = 0;
        input_buf_wr_en = 0;
        input_buf_data_in = 0;
        input_buf_read_addr = 0;
        input_buf_rd_en = 0;
        cntl_sig = 0;
        weight_memory_wr_en = 0;
        weight_memory_rd_en = 0;
        #10;

        for (int i = 0; i < 4; i = i + 1) begin
            $display("data write");
            #10 input_buf_wr_en = 1; input_buf_data_in = ~input_buf_data_in;
        end

        #10 input_buf_wr_en = 0;

        #10 input_buf_read_addr = 0;
//
        for (int i = 0; i < 4; i = i + 1) begin
            $display("data read");
            #10 input_buf_rd_en = 1;
            input_buf_read_addr = i;
            $display("input_buf_data_out: %d", input_buf_data_out);
        end

        #10 input_buf_wr_en = 0;

        #10;
        cntl_sig = 1;    // writing weights
        weight_memory_wr_en =1;
        #10;

        for (int i=0; i<25; i = i+1) begin
//            weight_val = ~weight_val;
            weight_memory_data_in = 1 | i;
            #10;
        end
        #10 weight_memory_wr_en = 0;

        #10 cntl_sig = 0;

        #10 cntl_sig = 2; // reading weights
//        weight_memory_rd_en = 1;


        $display("weights reading: ");
        for (int i = 0; i < 25; i = i + 1) begin
//            $display("weights reading: ");
            #10 weight_memory_rd_en = 1;
//            input_buf_read_addr = i;
            $display("weight_memory_data_out: %d", weight_memory_data_out);
        end
        #10 weight_memory_rd_en = 0;

        #10 cntl_sig = 0;

        #10000;
        // Apply reset
        // #10 reset_n = 1;
        // #10 reset_n = 0;
        // #10 reset_n = 1;

        // // Enable the counter
//        #10;
//        input_buf_wr_en = 1;
//        input_buf_data_in = 1;
//        #20;
//
//        // // Disable the counter
//        input_buf_wr_en = 0;
//        #20;
//
//        // // Enable the counter again
//        input_buf_wr_en = 1;
//        input_buf_data_in = 0;
//        #20;
//
//        input_buf_wr_en = 0;
//        #20;
//
//        input_buf_wr_en = 1;
//        input_buf_data_in = 1;
//        #20;

        // Finish the simulation
        #20 $finish;
    end

endmodule




//// ===========================================================
//// Testbench for Neuromorphic_design module
//// ===========================================================
//
//`timescale 1ns/1ps
//
//module Neuromorphic_design_tb;
//
//    // Inputs
//    logic clk_i;
//    logic rst_i;
//    logic input_buf_wr_en;
////    logic input_buf_data;
//
////    logic [31:0] addr;
////    logic output_buf_rd_en;
////    logic output_buf_data;
//    logic [31:0] dbg_out_wr_ptr;
//
//    // Instantiate the DUT (Design Under Test)
//    Neuromorphic_design uut (
//        .clk_i(clk_i),
//        .rst_i(rst_i),
//        .input_buf_wr_en(input_buf_wr_en),
////        .input_buf_data_in(input_buf_data),
////        .input_buf_read_addr(addr),
////        .input_buf_rd_en(output_buf_rd_en),
////        .input_buf_data_out(output_buf_data),
//        .dbg_out_wr_ptr(dbg_out_wr_ptr)
//    );
//
//    // Clock generation
//    initial begin
//        clk_i = 0;
//        forever #5 clk_i = ~clk_i; // 10ns period clock
//    end
//
//    // Test sequence
//    initial begin
//        // Open a VCD file for GTKWave
//        $dumpfile("Neuromorphic_design.vcd");
//        $dumpvars(0, Neuromorphic_design_tb);
//
//        // Initialize inputs
//        rst_i = 1;
//        input_buf_wr_en = 0;
////        input_buf_data = 0;
////        addr = 0;
////        output_buf_rd_en = 0;
////        output_buf_data = 0;
//
//        // Apply reset
//        #5;
//        rst_i = 0;
//        #5;
//        input_buf_wr_en = 1;
//        #10;
//        input_buf_wr_en = 0;
//        #10;
//        input_buf_wr_en = 1;
//        #100;
//
//        // Test case 1: Write to the buffer
////        input_buf_wr_en = 1;
////        input_buf_data = 1;
////        #10;
////        input_buf_wr_en = 1;
////        input_buf_data = 0;
////        #10;
////        input_buf_wr_en = 1;
////        input_buf_data = 1;
////        #10;
////        input_buf_wr_en = 1;
////        input_buf_data = 0;
////        #10;
//
////        output_buf_rd_en = 1;
//
////        for (int i=0; i<32; i=i+1) begin
////            $display("addr %d: val %d ", output_buf_data, addr[i]);
////        end
//
//        // Test case 3: Read from the buffer
////        addr = 1;
////        output_buf_rd_en = 1;
////        #20;
////        output_buf_rd_en = 0;
////        #20;
////        addr = 2;
////        output_buf_rd_en = 1;
////        #20;
////        output_buf_rd_en = 0;
////        #20;
////        addr = 3;
////        output_buf_rd_en = 1;
////        #20;
////        output_buf_rd_en = 0;
//
//
//        // Test case 2: Check buffer write and spike scheduler
//        // Add specific checks for your design as required
//
//        // End simulation
//        $finish;
//    end
//
//endmodule