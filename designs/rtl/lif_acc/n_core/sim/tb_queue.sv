`timescale 1ns/1ps

//==========================================================
// TB для Нейроморфного ядра 
//==========================================================

module tb_queue;

    localparam NUM_SPIKES        = 10;
  localparam SPIKE_WIDTH       = 8;   


  //==================================================
  // Объявление сигналов тестбенча
  //==================================================
  logic clk;
  logic rst_i;
  logic wr_input_queue;
  logic [SPIKE_WIDTH-1:0] w_data_input_queue;
  logic [SPIKE_WIDTH-1:0] r_data_input_queue;
  logic rd_input_queue;
  logic [3:0] rd_data_output_queue;
  logic empty_input_queue;
  logic empty_output_queue;
  logic full_input_queue;
  logic en_core;



  n_core dut (
	 .clk_i(clk),
	 .rst_i(rst_i),
	 .wr_input_queue(wr_input_queue),
	 .wr_data_input_queue(w_data_input_queue),
	 .full_input_queue(full_input_queue),
    //  .rd_input_queue(rd_input_queue),
	 .en_core(en_core)
  );


  //==================================================
  // Генератор тактового сигнала
  //==================================================
  initial begin
    clk = 1'b0;
    forever #5 clk = ~clk; // 5 ns 
  end

  //==================================================
  // Инициализация массива входных данных (имитация потока спайков)
  //==================================================
  logic [SPIKE_WIDTH-1:0] input_data_queue [NUM_SPIKES];

  initial begin
    integer i;
    for (i = 0; i < NUM_SPIKES; i = i + 1) begin
      input_data_queue[i] = 15; // i + 1;  // Заполняем тестовыми значениями
    end
  end

  //==================================================
  // Запись массива данных в FIFO
  //==================================================
  task write_input_queue_data(input int nums);
    integer i;
    begin
      wr_input_queue = 0;
      for (i = 0; i < nums; i = i + 1) begin
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

task read_input_queue_data(input int nums);
    integer i;
    begin
      dut.rd_input_queue = 0;
      for (i = 0; i < nums; i = i + 1) begin
        // r_data_input_queue = input_data_queue[i];
        dut.rd_input_queue = 1;
        @(posedge clk);
        // wr_input_queue     = 1;
        // @(posedge clk);
        // wr_input_queue     = 0;
      end
      dut.rd_input_queue = 0;
    end
  endtask

  //==================================================
  // Чтение пакета спайков из исходящей очереди
  //==================================================
  task read_output_spikes();
    integer i;
    i = 0;
    forever begin
    //   if (~empty_output_queue) 
      rd_input_queue = 1;
    //   else rd_input_queue = 0;
      @(posedge clk);
      // if (!$isunknown(rd_output_queue)) begin
        $display("%0d. Output Spike: %0d", i, rd_data_output_queue);
      // end 
      i += 1;
      // if (i == POSTSYN_NEURONS) $finish;
    end
  endtask
  

    //==================================================
  // Основные блоки стимулов
  //==================================================
  // Процедура записи тестовых пакетов спайков
  initial begin
    int spikes;
    wait (en_core == 1);
    @(posedge clk);
    // forever begin
      spikes = NUM_SPIKES;
      write_input_queue_data(spikes);
      spikes = 0;

      wait (dut.tick == 1);
      @(posedge clk);

    //   spikes = NUM_SPIKES;
    //     read_input_queue_data(spikes);
    //   spikes = 0;
    //   wait (dut.tick == 1);
    //   @(posedge clk);

    //  wait (dut.tick == 1);
    //   @(posedge clk);

      spikes = NUM_SPIKES;
      write_input_queue_data(spikes);
      spikes = 0;

     wait (dut.tick == 1);
      @(posedge clk);
 
    //       spikes = NUM_SPIKES;
    //     read_input_queue_data(spikes);
    //   spikes = 0;
    //   #10;
      
      spikes = NUM_SPIKES;
      write_input_queue_data(spikes);
      spikes = 0;


      wait (dut.tick == 1);
      @(posedge clk);

    // end
  end





  initial begin
    // --- Инициализация параметров модели ---
    // init_params(
    //     POSTSYN_NEURONS, 
    //     kLEAKAGE, 
    //     kTHRESHOLD, 
    //     kRESET
    //   );
    // --- Инициализация ядра ---
    rst_i               = 1;
    en_core             = 0;
    // wr_input_queue      = 0;
    dut.rd_input_queue      = 0;
    // w_data_input_queue  = 0;
    
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

  endmodule