`timescale 1ns/1ps

module Neuromorphic_design_tb;

  // Порты для подключения к тестируемому модулю
  logic unsigned [0:0] clk_i;
  logic unsigned [0:0] rst_i;

  // Параметры для симуляции
  parameter CLK_PERIOD = 10; // Период тактового сигнала 10 нс

  // Экземпляр тестируемого модуля
  Neuromorphic_design uut (
    .clk_i(clk_i),
    .rst_i(rst_i)
  );

  // Генерация тактового сигнала
  always #(CLK_PERIOD / 2) clk_i = ~clk_i;

  // Процесс инициализации и тестирования
  initial begin
    // Инициализация сигналов
    clk_i = 0;
    rst_i = 1;

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

    // Наблюдаем за сигналами некоторое время
    #50000;

    // Завершение симуляции
    $finish;
  end

  // Отслеживание сигналов в консоли
  always @(posedge clk_i) begin
    // $display("Time: %0t | tick: %0d, tick_period: %0d, clk_counter: %0d, next_clk_count: %0d, input_spike: %0d",
    //          $time, tick, tick_period, clk_counter, next_clk_count, input_spike);
  end

endmodule