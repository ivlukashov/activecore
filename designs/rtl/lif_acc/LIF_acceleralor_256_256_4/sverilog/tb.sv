`timescale 1ns/1ps

module Neuromorphic_design_tb;

// Входные сигналы для DUT
    reg clk;
    reg rst;
    reg [2:0] presyn_cntl_sig;
    reg [2:0] postsyn_cntl_sig;  // Добавлен сигнал для управления постсинаптическим счётчиком
    reg dbg_sig_rd_fifo;

// Выходные сигналы DUT
    wire [0:0] fifo_output_genfifo_req_o;
    wire [0:0] fifo_output_genfifo_wdata_bo [3:0];

// Инстанцируем DUT
    Neuromorphic_design dut (
        .clk_i(clk),
        .rst_i(rst),
        .presyn_cntl_sig(presyn_cntl_sig),
        .postsyn_cntl_sig(postsyn_cntl_sig),  // Подключение постсинаптического сигнала
        .dbg_sig_rd_fifo(dbg_sig_rd_fifo),
        .fifo_output_genfifo_req_o(fifo_output_genfifo_req_o)
    );

// Генерация сигнала clk
    always #5 clk = ~clk;

    initial begin
        // Инициализация сигналов
        clk = 0;
        rst = 1;
        presyn_cntl_sig = 0;
        postsyn_cntl_sig = 0;  // Инициализация постсинаптического сигнала
        dbg_sig_rd_fifo = 0;

        // Мониторинг значений
        $monitor("Time=%0t | presyn_neuron_counter_num=%0d | postsyn_neuron_counter_num=%0d | presyn_counter_done=%d | postsyn_counter_done=%d",
            $time, dut.presyn_neuron_counter_num, dut.postsyn_neuron_counter_num, dut.presyn_counter_done, dut.postsyn_counter_done);

        // Сброс
        #10 rst = 0;

        // Запуск пресинаптического счётчика
        $display("Starting presynaptic counter...");
        #20 presyn_cntl_sig = 3'b000;  // idle
        #200 presyn_cntl_sig = 3'b001;  // start
        #1 presyn_cntl_sig = 3'b010;  // pause
        #1 presyn_cntl_sig = 3'b001;  // pause

        #200 presyn_cntl_sig = 3'b011;  // reset

        // Запуск постсинаптического счётчика
        $display("Starting postsynaptic counter...");
        #20 postsyn_cntl_sig = 3'b000;  // idle
        #200 postsyn_cntl_sig = 3'b001;  // start
//        #200 postsyn_cntl_sig = 3'b011;  // reset

        // Окончание симуляции
        #1000;
        $finish;
    end

endmodule