`timescale 1ns/1ps

//==========================================================
// Testbench для параметризированного FIFO модуля
//==========================================================

// `timescale 1ns/1ps

module tb_fifo;

// Локальные параметры для удобства переопределения
localparam B = 8;  // ширина данных
localparam W = 4;  // размер адресных указателей (FIFO глубиной 2^W)

// Сигналы тестбенча
logic               clk;
logic               reset_input_queue;
logic               rd_input_queue;
logic               wr_input_queue;
logic [B-1:0]       w_data_input_queue;
wire [B-1:0]        r_data_input_queue;
wire                empty_input_queue;
wire                full_input_queue;
logic               en_core;
logic               rst_i;

// Инстанцирование DUT (Device Under Test)
n_core dut (
    .clk_i    (clk),
    .rst_i     (rst_i),
    .reset_input_queue  (reset_input_queue),
    .rd_input_queue     (rd_input_queue),
    .wr_input_queue     (wr_input_queue),
    .w_data_input_queue (w_data_input_queue),
    .r_data_input_queue (r_data_input_queue),
    .empty_input_queue  (empty_input_queue),
    .full_input_queue   (full_input_queue),
    .en_core(en_core)
);

// Генератор тактового сигнала
initial begin
    clk = 1'b0;
    forever #5 clk = ~clk;  // Период 10 нс
end

localparam N = 32; // Размерность синаптической памяти

// Инициализация памяти weights_mem внутри DUT
initial begin
    integer i, j;
    @(posedge reset_input_queue);  // Ждём сброса
    @(negedge reset_input_queue);  // После снятия reset_input_queue начинаем инициализацию

    for (i = 0; i < N; i = i + 1) begin
        for (j = 0; j < N; j = j + 1) begin
            dut.weights_mem[i][j] = (i == j) ? 8'h10 : 8'h05; // Диагональные веса больше
        end
    end
end

localparam width_spike = 8;
localparam nums_spikes = 32;
// Массив входных данных (имитация входного потока спайков)
logic [width_spike-1:0] input_data_queue [nums_spikes-1:0];

initial begin
    integer i;
    for (i = 0; i < nums_spikes; i = i + 1) begin
        input_data_queue[i] = i;  // Заполняем тестовыми значениями
    end
end

// Основной блок стимулов
initial begin
    // Изначально сбросим все сигналы
    reset_input_queue = 1;
    rst_i = 1;
    en_core = 0;
    wr_input_queue    = 0;
    rd_input_queue    = 0;
    w_data_input_queue = '0;
    
    // Удерживаем reset_input_queue активным некоторое время
    #20;
    reset_input_queue = 0;
    rst_i = 0;

    // Небольшая пауза после снятия reset_input_queue
    #10;
    
    // Включаем ядро
    en_core = 1;

    // Пишем входящие спайки спайков в входную очередь
    // wr_input_queueite_data(8'hAA);
    // wr_input_queueite_data(8'hBB);
    // wr_input_queueite_data(8'hCC);

    wr_input_queue_data();
    
    // Останавливаем запись
    wr_input_queue = 0;
    
    // Делаем небольшую паузу
    #10;
    
    // // Считываем значения
    // read_data();
    // read_data();
    // read_data();
    
    // // Останавливаем чтение
    // rd_input_queue = 0;
    
    // Завершаем симуляцию
    #20;
    $finish;
end

// Процедура записи одного байта в FIFO
task wr_input_queueite_data(input [B-1:0] data);
    begin
        @(posedge clk);
        w_data_input_queue = data;
        wr_input_queue     = 1;
        // Гарантированно отправляем хотя бы один такт с установленным wr_input_queue
        @(posedge clk);
        wr_input_queue     = 0;
    end
endtask

// Процедура записи **массива данных** в FIFO
task wr_input_queue_data();
    integer i;
    begin
        for (i = 0; i < N; i = i + 1) begin
            @(posedge clk);
            w_data_input_queue = input_data_queue[i];
            wr_input_queue     = 1;
            @(posedge clk);
            wr_input_queue     = 0;
        end
    end
endtask

// Процедура чтения одного байта из FIFO
task read_data();
    begin
        @(posedge clk);
        rd_input_queue = 1;
        // Держим rd_input_queue хотя бы один такт
        @(posedge clk);
        rd_input_queue = 0;
    end
endtask

// Мониторинг основных сигналов
always @(posedge clk) begin
    $display("[%0t] reset_input_queue=%0b, wr_input_queue=%0b, rd_input_queue=%0b, w_data_input_queue=0x%0h, r_data_input_queue=0x%0h, full_input_queue=%0b, empty_input_queue=%0b",
             $time, reset_input_queue, wr_input_queue, rd_input_queue, w_data_input_queue, r_data_input_queue, full_input_queue, empty_input_queue);
end

endmodule


// module Neuromorphic_design_tb;

//     localparam CLK_PERIOD = 10;

//     bit clk;
//     bit rst;
//     bit en;

//     // Переменные для преобразованных значений
//     // real membr_pot_mem_float_0;
//     // real membr_pot_mem_float_1;
//     // real membr_pot_mem_float_2;
//     // real membr_pot_mem_float_3;
//     // real membr_pot_mem_float_4;
//     // real membr_pot_mem_float_5;
//     // real membr_pot_mem_float_6;
//     // real membr_pot_mem_float_7;
//     // real membr_pot_mem_float_8;
//     // real membr_pot_mem_float_9;

//     // real weights_mem_float_0;
//     // real weights_mem_float_1;
//     // real weights_mem_float_2;
//     // real weights_mem_float_3;
//     // real weights_mem_float_4;
//     // real weights_mem_float_5;
//     // real weights_mem_float_6;
//     // real weights_mem_float_7;
//     // real weights_mem_float_8;
//     // real weights_mem_float_9;


//     n_core dut (
//         .clk_i(clk),
//         .rst_i(rst),
//         .en_core(en)
//     );

// //     // Функция для преобразования фиксированной точки в плавающую
// //     function real fixed_to_float(input signed [15:0] fixed_value);
// //         begin
// // //            fixed_to_float = fixed_value / 256.0;  // Преобразуем число с 8-битной дробной частью
// // //            fixed_to_float = fixed_value / 8192.0;  // Преобразуем число с 8-битной дробной частью
// //             fixed_to_float = fixed_value / 16384.0;

// //         end
// //     endfunction

//     // // Преобразование и обновление переменных для мониторинга
//     // always @(posedge clk) begin
//     //     membr_pot_mem_float_0 = fixed_to_float(dut.membrane_potential_memory[0]);
//     //     membr_pot_mem_float_1 = fixed_to_float(dut.membrane_potential_memory[1]);
//     //     membr_pot_mem_float_2 = fixed_to_float(dut.membrane_potential_memory[2]);
//     //     membr_pot_mem_float_3 = fixed_to_float(dut.membrane_potential_memory[3]);
//     //     membr_pot_mem_float_4 = fixed_to_float(dut.membrane_potential_memory[4]);
//     //     membr_pot_mem_float_5 = fixed_to_float(dut.membrane_potential_memory[5]);
//     //     membr_pot_mem_float_6 = fixed_to_float(dut.membrane_potential_memory[6]);
//     //     membr_pot_mem_float_7 = fixed_to_float(dut.membrane_potential_memory[7]);
//     //     membr_pot_mem_float_8 = fixed_to_float(dut.membrane_potential_memory[8]);
//     //     membr_pot_mem_float_9 = fixed_to_float(dut.membrane_potential_memory[9]);
//     // end

//     // always @(posedge clk) begin
//     //     weights_mem_float_0 = fixed_to_float(dut.weights_mem[0][0]);
//     //     weights_mem_float_1 = fixed_to_float(dut.weights_mem[0][1]);
//     //     weights_mem_float_2 = fixed_to_float(dut.weights_mem[0][2]);
//     //     weights_mem_float_3 = fixed_to_float(dut.weights_mem[0][3]);
//     //     weights_mem_float_4 = fixed_to_float(dut.weights_mem[0][4]);
//     //     weights_mem_float_5 = fixed_to_float(dut.weights_mem[0][5]);
//     //     weights_mem_float_6 = fixed_to_float(dut.weights_mem[0][6]);
//     //     weights_mem_float_7 = fixed_to_float(dut.weights_mem[0][7]);
//     //     weights_mem_float_8 = fixed_to_float(dut.weights_mem[0][8]);
//     //     weights_mem_float_9 = fixed_to_float(dut.weights_mem[0][9]);
//     // end

//     always #5 clk = ~clk;

//     // initial begin
//     //     $readmemh("weights.dat", dut.weights_mem);
//     //     $readmemb("fifo_data.txt", dut.in_fifo);

//     //     @(negedge rst);
//     // end


//     initial begin
//         clk = 0;
//         rst = 1;
//         en = 0;

//         $monitor("Time=%0t | curr_state=%0d", $time, dut.current_state); // Мониторим состояние in_fifo


// //        $monitor("Time=%0t | curr_state=%0d | in_spk_num=%0d, membr_pot_mem[0]=%.8f, membr_pot_mem[1]=%.8f, membr_pot_mem[2]=%.8f, membr_pot_mem[3]=%.8f, membr_pot_mem[4]=%.8f, membr_pot_mem[5]=%.8f, membr_pot_mem[6]=%.8f, membr_pot_mem[7]=%.8f, membr_pot_mem[8]=%.8f, membr_pot_mem[9]=%.8f, weight_presyn_idx=%0d | weight_postsyn_idx=%0d | presyn_num=%0d | postsyn_num=%0d | weight_upd=%0d, out=%0d%0d%0d%0d%0d%0d%0d%0d%0d%0d, in=%0d, %0d",
// //            $time, dut.current_state, dut.input_spike_num,
// //            membr_pot_mem_float_0, membr_pot_mem_float_1, membr_pot_mem_float_2, membr_pot_mem_float_3, membr_pot_mem_float_4,
// //            membr_pot_mem_float_5, membr_pot_mem_float_6, membr_pot_mem_float_7, membr_pot_mem_float_8, membr_pot_mem_float_9,
// //            dut.weight_presyn_idx, dut.weight_postsyn_idx, dut.presynapse_neuron_number, dut.postsynapse_neuron_number,
// //            dut.weight_upd, dut.out_fifo[0], dut.out_fifo[1], dut.out_fifo[2], dut.out_fifo[3], dut.out_fifo[4], dut.out_fifo[5], dut.out_fifo[6], dut.out_fifo[7], dut.out_fifo[8], dut.out_fifo[9], dut.in_fifo[0], dut.in_fifo[4]); // Мониторим состояние in_fifo

// //        $monitor("Time=%0t | w0=%0d | w1=%0d, w2=%.8f, w3=%.8f, w4=%.8f, w5=%.8f, w6=%.8f, w7=%.8f, w8=%.8f,  w9=%.8f",
// //            $time,  weights_mem_float_0, weights_mem_float_1, weights_mem_float_2, weights_mem_float_3, weights_mem_float_4, weights_mem_float_5, weights_mem_float_6, weights_mem_float_7, weights_mem_float_8, weights_mem_float_9); // Мониторим состояние in_fifo


//         #10 rst = 0;
//         #20 en = 1;
//         #150 en = 0;
//         #1000;
//         $finish;
//     end

//     initial begin
//         $dumpfile("Neuromorphic_design.vcd");
//         $dumpvars(0, Neuromorphic_design_tb);
//         // $dumpvars(0, dut.presynapse_neuron_number);
//     end

// endmodule : Neuromorphic_design_tb