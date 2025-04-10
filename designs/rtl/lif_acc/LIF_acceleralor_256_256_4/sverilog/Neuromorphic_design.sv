// ===========================================================
// RTL generated by ActiveCore framework
// Date: 2024-09-28 15:42:31
// Copyright Alexander Antonov <antonov.alex.alex@gmail.com>
// ===========================================================

`include "Neuromorphic_design.svh"

module Neuromorphic_design (
	input logic unsigned [0:0] clk_i
	, input logic unsigned [0:0] rst_i
);


logic unsigned [31:0] tick;
logic unsigned [31:0] tick_period;
logic unsigned [31:0] clk_counter;
logic unsigned [31:0] next_clk_count;
logic unsigned [31:0] input_spike;
logic unsigned [0:0] gen4_cyclix_genvar;
logic unsigned [0:0] gen5_cyclix_genvar;
logic unsigned [32:0] gen6_cyclix_genvar;
logic unsigned [0:0] gen7_cyclix_genvar;
logic unsigned [0:0] gen0_rtl_var;
logic unsigned [0:0] gen1_rtl_var;

logic unsigned [31:0] gensticky_tick;
always_ff @(posedge clk_i)
	if (rst_i)
		begin
		gensticky_tick <= 32'd0;
		end
	else
		begin
		gensticky_tick <= tick;
		end

logic unsigned [31:0] gensticky_tick_period;
always_ff @(posedge clk_i)
	if (rst_i)
		begin
		gensticky_tick_period <= 32'd5;
		end
	else
		begin
		gensticky_tick_period <= tick_period;
		end

logic unsigned [31:0] gensticky_clk_counter;
always_ff @(posedge clk_i)
	if (rst_i)
		begin
		gensticky_clk_counter <= 32'd0;
		end
	else
		begin
		gensticky_clk_counter <= clk_counter;
		end

logic unsigned [31:0] gensticky_next_clk_count;
always_ff @(posedge clk_i)
	if (rst_i)
		begin
		gensticky_next_clk_count <= 32'd0;
		end
	else
		begin
		gensticky_next_clk_count <= next_clk_count;
		end

logic unsigned [31:0] gensticky_input_spike;
always_ff @(posedge clk_i)
	if (rst_i)
		begin
		gensticky_input_spike <= 32'd0;
		end
	else
		begin
		gensticky_input_spike <= input_spike;
		end


always_comb
	begin
	tick = 32'd0;
	gen6_cyclix_genvar = 32'd0;
	next_clk_count = 32'd0;
	clk_counter = 32'd0;
	tick_period = gensticky_tick_period;
	tick = gensticky_tick;
	next_clk_count = gensticky_next_clk_count;
	clk_counter = gensticky_clk_counter;
	input_spike = gensticky_input_spike;
	// Buffering globals
	// Processing dlychains
	// fifo_in buffering
	// subproc fifo_in buffering
	// Payload logic
	tick_period = 32'd5;
	gen4_cyclix_genvar = (tick_period != clk_counter);
	gen5_cyclix_genvar = gen4_cyclix_genvar;
	gen0_rtl_var = gen5_cyclix_genvar;
	if (gen0_rtl_var)
		begin
		tick = 32'd0;
		gen6_cyclix_genvar = (clk_counter + 32'd1);
		next_clk_count = gen6_cyclix_genvar;
		clk_counter = next_clk_count;
		end
	gen7_cyclix_genvar = 1'd0;
	gen7_cyclix_genvar = (gen7_cyclix_genvar || gen5_cyclix_genvar);
	gen7_cyclix_genvar = !gen7_cyclix_genvar;
	gen1_rtl_var = gen7_cyclix_genvar;
	if (gen1_rtl_var)
		begin
		tick = 32'd1;
		clk_counter = 32'd0;
		end
	input_spike = 32'd1;
	end


endmodule
