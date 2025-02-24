`timescale 1ns/1ps

module Neuromorphic_design_tb;

  // Порты для подключения к тестируемому модулю
  logic unsigned [0:0] clk_i;
  logic unsigned [0:0] rst_i;
  logic unsigned [0:0] membrane_potential_syn_start;

  // Параметры для симуляции
  parameter CLK_PERIOD = 10; // Период тактового сигнала 10 нс

  // Экземпляр тестируемого модуля
  Neuromorphic_design uut (
    .clk_i(clk_i),
    .rst_i(rst_i) ,
    // .membrane_potential_start(membrane_potential_start),
    .membrane_potential_syn_start(membrane_potential_syn_start)
  );

  // Генерация тактового сигнала
  always #(CLK_PERIOD / 2) clk_i = ~clk_i;

  // Процесс инициализации и тестирования
  initial begin
    // Инициализация сигналов
    clk_i = 0;
    rst_i = 1;
    // membrane_potential_start = 0; 
    // membrane_potential_syn_start = 0;

    // Открываем файл для VCD
    $dumpfile("Neuromorphic_design_tb.vcd");
    $dumpvars(0, Neuromorphic_design_tb);
    $dumpvars(0, uut);



    // Старт тестирования
    #20; // Задержка для начальной стабилизации

    // Сброс системы
    rst_i = 0;
    #100;
    
    // Поднимаем сигнал сброса для перезапуска
    rst_i = 1;
    #20;
    rst_i = 0;
    // membrane_potential_start = 1; 
    membrane_potential_syn_start = 1;
    
    // for (int i=0; i<10; i=i+1) begin
    //   uut.presyn_membrane_potential[i] = 1;
    // end

    for (int i=0; i<10; i=i+1) begin
      for (int j=0; j<10; j=j+1) begin 
        uut.weight_static[j][i] = 1;
      end
    end   

    // Наблюдаем за сигналами некоторое время
    #1000;

    // Завершение симуляции
    $finish;
  end

  // Отслеживание сигналов в консоли
  always @(posedge clk_i) begin
    $display("Time: %0t |  m0:%0d m1: %0d, m2: %0d, m3: %0d, m4: %0d, m5 %0d, w1: %0d, w2: %0d, w3: %0d, w4: %0d ",
             $time, uut.postsyn_membrane_potential[0], uut.postsyn_membrane_potential[1], uut.postsyn_membrane_potential[2], uut.postsyn_membrane_potential[3], uut.postsyn_membrane_potential[4], uut.postsyn_membrane_potential[5], uut.weight_static[0][0], uut.weight_static[0][1], uut.weight_static[0][2], uut.weight_static[0][3]);
  end

endmodule