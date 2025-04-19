`timescale 1ns/1ps

//==========================================================
// TB для Нейроморфного ядра 
//==========================================================

module tb;

  //==================================================
  // Параметры
  //==================================================
  localparam PRESYN_NEURONS    = 16;  // Количество пресинаптических нейронов
  localparam POSTSYN_NEURONS   = 16;  // Количество постсинаптических нейронов
  localparam SPIKE_WIDTH       = 4;   
  localparam NUM_SPIKES        = 10; // (PRESYN_NEURONS - 5);  // Размер входной очереди спайков

  localparam WEIGHT_WIDTH      = 8;
  localparam WEIGHT_ADDR_WIDTH = 8;  // Разрядность адреса памяти весов = {log2(PRESYN_NEURONS), log2(POSTSYN_NEURONS)} = {4,4}
  localparam WEIGHTS_DEPTH     = PRESYN_NEURONS * POSTSYN_NEURONS;

  localparam INIT_WEIGHTS_DATA = "/home/yan/activecore/designs/rtl/lif_acc/n_core/sim/weights_data.hex";  // [WEIGHT_WIDTH][WEIGHTS_DEPTH] = [8][1024]
  // localparam INPUT_SPIKES_DATA = "/home/yan/activecore/designs/rtl/lif_acc/n_core/sim/input_spikes.hex"
  
  //==================================================
  // Параметры целевой модели ИмНС
  //==================================================

  localparam kLEAKAGE          = 1;
  localparam kTHRESHOLD        = 5;
  localparam kRESET            = 0;


  //==================================================
  // Объявление сигналов тестбенча
  //==================================================
  logic clk;
  logic rst_i;
  logic wr_input_queue;
  logic [3:0] w_data_input_queue;
  logic [SPIKE_WIDTH-1:0] r_data_input_queue;
  logic rd_input_queue;
  logic [3:0] rd_data_output_queue;
  logic empty_input_queue;
  logic empty_output_queue;
  logic full_input_queue;
  logic en_core;

  logic [WEIGHT_ADDR_WIDTH-1:0] weight_mem_adr_i;
  logic [WEIGHT_WIDTH-1:0] weight_mem_dat_o;

  //==================================================
  // Инстанцирование DUT (n_core)
  //==================================================
  n_core dut (
	 .clk_i(clk),
	 .rst_i(rst_i),
	 .wr_input_queue(wr_input_queue),
	 .wr_data_input_queue(w_data_input_queue),
	 .full_input_queue(full_input_queue),
	 .rd_output_queue(rd_output_queue),
	 .empty_output_queue(empty_output_queue),
	 .rd_data_output_queue(rd_data_output_queue),
   .adr_ram_i(weight_mem_adr_i),
   .dat_ram_o(weight_mem_dat_o),
	 .en_core(en_core)
  );

  //==================================================
  // Инстанцирование RAM для весов
  //==================================================

  ram #(  // Модуль RAM на Verilog
    WEIGHT_WIDTH,   // Ширина данных
    WEIGHT_ADDR_WIDTH,   // Ширина адреса
    WEIGHTS_DEPTH  // Размер памяти
  ) ram_weights (
    .adr_i (weight_mem_adr_i), // Адрес
    .we_i  (1'b0),            // Запись отключена
    .dat_o (weight_mem_dat_o),// Выход данных
    .clk   (clk)
  );

  //==================================================
  // Генератор тактового сигнала
  //==================================================
  initial begin
    clk = 1'b0;
    forever #5 clk = ~clk; // 5 ns 
  end

  //==================================================
  // Инициализация памяти весов внутри DUT
  //==================================================
  initial begin
    $readmemh(INIT_WEIGHTS_DATA, ram_weights.ram, 0);
  end

  //==================================================
  // Инициализация массива входных данных (имитация потока спайков)
  //==================================================
  logic [SPIKE_WIDTH-1:0] input_data_queue [NUM_SPIKES-1:0];

  initial begin
    integer i;
    for (i = 0; i < NUM_SPIKES; i = i + 1) begin
      input_data_queue[i] = 15; // i + 1;  // Заполняем тестовыми значениями
    end
  end

  //==================================================
  // Запись одного байта в FIFO
  //==================================================
  task write_single_input_data(input [SPIKE_WIDTH-1:0] data);
    begin
      @(posedge clk);
      w_data_input_queue = data;
      wr_input_queue     = 1;
      @(posedge clk);
      wr_input_queue     = 0;
    end
  endtask

  //==================================================
  // Запись массива данных в FIFO
  //==================================================
  task write_input_queue_data(input int nums);
    integer i;
    begin
      wr_input_queue = 0;
      for (i = 0; i <= nums; i = i + 1) begin
        w_data_input_queue = input_data_queue[i];
        wr_input_queue = 1;
        @(posedge clk);
        // wr_input_queue     = 1;
        // @(posedge clk);
        // wr_input_queue     = 0;
      end
      wr_input_queue = 0;
    end
  endtask

  //==================================================
  // Чтение одного байта из FIFO
  //==================================================
  task read_data();
    begin
      @(posedge clk);
      rd_input_queue = 1;
      @(posedge clk);
      rd_input_queue = 0;
    end
  endtask

  //==================================================
  // Чтение пакета спайков из исходящей очереди
  //==================================================
  task read_output_spikes();
    integer i;
    i = 0;
    forever begin
      if (~empty_output_queue) rd_input_queue = 1;
      else rd_input_queue = 0;
      @(posedge clk);
      // if (!$isunknown(rd_output_queue)) begin
        $display("%0d. Output Spike: %0d", i, rd_data_output_queue);
      // end 
      i += 1;
      // if (i == POSTSYN_NEURONS) $finish;
    end
  endtask

  //==================================================
  // Инициализация параметров модели
  //==================================================
  task init_params(
      input int postsyn_neurons_num, 
      input int leakage_cur, 
      input int threshold_vol, 
      input int reset_vol
    );
    begin
      dut.PostsynNeuronsNumsParam = postsyn_neurons_num;
      dut.LeakageParam = leakage_cur;
      dut.ThresholdParam = threshold_vol;
      dut.ResetParam = reset_vol;
    end
  endtask

  //==================================================
  // Основные блоки стимулов
  //==================================================
  // Процедура записи тестовых пакетов спайков
  initial begin
    int spikes;
    wait (en_core == 1);
    forever begin
      spikes = NUM_SPIKES;
      write_input_queue_data(spikes);
      spikes = 0;
      wait (dut.tick == 1);
      @(posedge clk);
    end
  end

  // Монитор мембранных потенциалов
  initial begin
    int tick_num;
    tick_num = 0;
    forever begin
      wait (dut.tick == 1);
      @(posedge clk);
      $display("Membrane potentials after tick %d:", tick_num);
      for (int i = 0; i < POSTSYN_NEURONS; i++) begin
        $display("Neuron %0d: Buf0=%0d, Buf1=%0d", 
            i, dut.l1_membrane_potential_memory_3d[i][0], 
            dut.l1_membrane_potential_memory_3d[i][1]);
      end
      tick_num += 1;
    end 
  end


  initial begin
    // --- Инициализация параметров модели ---
    init_params(
        POSTSYN_NEURONS, 
        kLEAKAGE, 
        kTHRESHOLD, 
        kRESET
      );
    // --- Инициализация ядра ---
    rst_i               = 1;
    en_core             = 0;
    wr_input_queue      = 0;
    rd_input_queue      = 0;
    w_data_input_queue  = 0;
    
    #20;
    rst_i               = 0;
    #10;
    

    // // --- Включение ядра ---
    en_core = 1;

    // // TODO: Мониторинг выходных данных  
    // // read_data();
    // read_output_spikes();
    
    // Задержка 
    #200000;
    $finish;
  end


  //==================================================
  // Мониторинг сигналов
  //==================================================
  // always @(posedge clk) begin
  //   $display("[%0t] wr_input_queue=%0b, rd_input_queue=%0b, w_data_input_queue=0x%0h, r_data_input_queue=0x%0h, full_input_queue=%0b, empty_input_queue=%0b",
  //            $time, wr_input_queue, rd_input_queue, 
  //            w_data_input_queue, r_data_input_queue, full_input_queue, empty_input_queue);
  // end

endmodule

