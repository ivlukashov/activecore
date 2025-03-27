/*
 * Neuromorphic.kt
 *     License: See LICENSE file for details
 */

package neuromorphix

import cyclix.*
import cyclix.RtlGenerator.fifo_in_descr
import hwast.*
import kotlin.math.ln
import javax.sound.sampled.Port
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.memberFunctions
import org.json.JSONObject
import java.io.File
import kotlin.math.pow


class TickGenerator {
    fun tick_generation(
        tick_signal: hw_var,
        timeslot: Int,
        units: String,
        clk_period: Int,
        cyclix_gen: Generic
    ) {  // timeslot in ms, clk_period in ns
        // Generating Tick for timeslot processing period
        var tick_period_val = 0
        if (units == "ns") {
            tick_period_val = clk_period * 1 * timeslot
            println(tick_period_val)
        } else if (units == "us"){
            tick_period_val = clk_period * 1000 * timeslot
            println(tick_period_val)
        } else if (units == "ms") {
            tick_period_val = clk_period * 1000000 * timeslot
            println(tick_period_val)
        } else if (units == "s") {
            tick_period_val = clk_period * 1000000000 * timeslot
            println(tick_period_val)
        }

        val tick_period = cyclix_gen.uglobal("tick_period", hw_imm(timeslot))
        val clk_counter = cyclix_gen.uglobal("clk_counter", "0")
        val next_clk_count = cyclix_gen.uglobal("next_clk_count", "0")

        tick_period.assign(tick_period_val)

        cyclix_gen.begif(cyclix_gen.neq2(tick_period, clk_counter))
        run {
            tick_signal.assign(0)
            next_clk_count.assign(clk_counter.plus(1))
            clk_counter.assign(next_clk_count)
        }; cyclix_gen.endif()

        cyclix_gen.begelse()
        run {
            tick_signal.assign(1)
            clk_counter.assign(0)
        }; cyclix_gen.endif()
    }
}


data class OutputQueueCounters(
    val wrCounter: hw_var,
    val rdCounter: hw_var,
    val w_data_if: hw_var,
    val wr_if: hw_var
)

data class InputQueueCounters(
    val rdCounter: hw_var,
    val wrCounter: hw_var,
    val r_data_if: hw_var,
    val rd_if: hw_var
)

data class InternalSpikeBufferCounters(
    val rdCounter: hw_var,
    val wrCounter: hw_var,
    val w_data_if: hw_var,
    val wr_if: hw_var,
    val r_data_if: hw_var,
    val rd_if: hw_var
)

class Queues {
    internal fun input_queue_generation_credit_counter(
        name: String,
        data_width: Int,
        pointer_width: Int,
        depth: Int,
        tick: hw_var,
        cyclix_gen: Generic
    ): InputQueueCounters{

            // Initialize interface signals as globals
            val r_data_if = cyclix_gen.uglobal("r_data_if_" + name, hw_dim_static(data_width), "0")
            val rd_if = cyclix_gen.uglobal("rd_if_" + name, hw_dim_static(1), "0")

        // Внешний сигнал
        val reset = cyclix_gen.uport("reset_"+name, PORT_DIR.IN, hw_dim_static(1), "0")
        val reset_r = cyclix_gen.uglobal("reset_r_"+name, hw_dim_static(1), "0")
//        reset.assign(reset_r)

        val rd = cyclix_gen.uport("rd_"+name, PORT_DIR.IN, hw_dim_static(1), "0")
        val rd_r = cyclix_gen.uglobal("rd_r_"+name, hw_dim_static(1), "0")
//        rd.assign(rd_r)

        val wr = cyclix_gen.uport("wr_"+name, PORT_DIR.IN, hw_dim_static(1), "0")
        val wr_r = cyclix_gen.uglobal("wr_r_"+name, hw_dim_static(1), "0")
//        wr.assign(wr_r)

        val w_data = cyclix_gen.uport("w_data_"+name, PORT_DIR.IN, hw_dim_static(data_width), "0")
        val w_data_r = cyclix_gen.uglobal("w_data_r_"+name, hw_dim_static(data_width), "0")
//        w_data.assign(w_data_r)

        val wr_counter_p = cyclix_gen.uport("wr_counter_p_"+name, PORT_DIR.OUT, hw_dim_static(8), "0")
        val wr_counter_p_r = cyclix_gen.uglobal("wr_counter_p_r_"+name, hw_dim_static(8), "0")
        wr_counter_p.assign(wr_counter_p_r)
        val rd_counter_p = cyclix_gen.uport("rd_counter_p_"+name, PORT_DIR.OUT, hw_dim_static(8), "0")
        val rd_counter_p_r = cyclix_gen.ulocal("rd_counter_p_r_"+name, hw_dim_static(8), "0")
        rd_counter_p.assign(rd_counter_p_r)
//
//        rd_cnt.assign(rd_counter_p_r)
//        wr_cnt.assign(wr_counter_p_r)


        val empty = cyclix_gen.uport("empty_"+name, PORT_DIR.OUT, hw_dim_static(1), "0")
        val empty_r = cyclix_gen.uglobal("empty_r_"+name, hw_dim_static(1), "0")
        empty.assign(empty_r)

        val full = cyclix_gen.uport("full_"+name, PORT_DIR.OUT, hw_dim_static(1), "0")
        val full_r = cyclix_gen.uglobal("full_r_"+name, hw_dim_static(1), "0")
        full_r.assign(full)

        val r_data = cyclix_gen.uport("r_data_"+name, PORT_DIR.OUT, hw_dim_static(data_width), "0")
        val r_data_r = cyclix_gen.uglobal("r_data_r_"+name, hw_dim_static(data_width), "0")
        r_data_r.assign(r_data)

        val array_reg_dim = hw_dim_static(data_width)
        array_reg_dim.add(depth, 0)
        var array_reg = cyclix_gen.uglobal("array_reg_"+name, array_reg_dim, "0")

        val w_ptr_reg = cyclix_gen.uglobal("w_ptr_reg_"+name, hw_dim_static(data_width-1), "0")
        val w_ptr_next = cyclix_gen.uglobal("w_ptr_next_"+name, hw_dim_static(data_width-1), "0")
        val w_ptr_succ = cyclix_gen.uglobal("w_ptr_succ_"+name, hw_dim_static(data_width-1), "0")

        val r_ptr_reg = cyclix_gen.uglobal("r_ptr_reg_"+name, hw_dim_static(data_width-1), "0")
        val r_ptr_next = cyclix_gen.uglobal("r_ptr_next_"+name, hw_dim_static(data_width-1), "0")
        val r_ptr_succ = cyclix_gen.uglobal("r_ptr_succ_"+name, hw_dim_static(data_width-1), "0")

        val full_reg = cyclix_gen.uglobal("full_reg_"+name, hw_dim_static(1), "0")
        val empty_reg = cyclix_gen.uglobal("empty_reg_"+name, hw_dim_static(1), "1")
        val full_next = cyclix_gen.uglobal("full_next_"+name, hw_dim_static(1), "0")
        val empty_next = cyclix_gen.uglobal("empty_next_"+name, hw_dim_static(1), "0")

        val wr_en = cyclix_gen.uglobal("wr_en_"+name, hw_dim_static(1), "0")

        val credit_counter_dim = hw_dim_static(8)
        credit_counter_dim.add(1,0)
        val credit_counter = cyclix_gen.uglobal("credit_counter_"+name, credit_counter_dim, "0")

        val act_counter = cyclix_gen.uglobal("act_counter"+name, hw_dim_static(0, 0), "0")

        cyclix_gen.begif(cyclix_gen.eq2(reset_r, 1))
        run{
            act_counter.assign(0)
        }; cyclix_gen.endif()

        cyclix_gen.begif(cyclix_gen.eq2(tick, 1))
        run{
            act_counter.assign(cyclix_gen.bnot(act_counter))
        }; cyclix_gen.endif()


        cyclix_gen.begif(cyclix_gen.eq2(wr_en, 1))
        run{
            array_reg[w_ptr_reg].assign(w_data_r)
            credit_counter[act_counter].assign(credit_counter[act_counter].plus(1))
        }; cyclix_gen.endif()

        wr_counter_p_r.assign(credit_counter[act_counter])
        rd_counter_p_r.assign(credit_counter[cyclix_gen.bnot(act_counter)])

        r_data_r.assign(array_reg[r_ptr_reg])

        wr_en.assign(cyclix_gen.land(wr_r, cyclix_gen.bnot(full_reg)))

        cyclix_gen.begif(cyclix_gen.eq2(reset_r, 1))
        run{
            w_ptr_reg.assign(0)
            r_ptr_reg.assign(0)
            full_reg.assign(0)
            empty_reg.assign(1)
        }; cyclix_gen.endif()
        cyclix_gen.begelse()
        run{
            w_ptr_reg.assign(w_ptr_next)
            r_ptr_reg.assign(r_ptr_next)
            full_reg.assign(full_next)
            empty_reg.assign(empty_next)
        }; cyclix_gen.endif()

        w_ptr_succ.assign(w_ptr_reg.plus(1))
        r_ptr_succ.assign(r_ptr_reg.plus(1))
        w_ptr_next.assign(w_ptr_reg)
        r_ptr_next.assign(r_ptr_reg)
        full_next.assign(full_reg)
        empty_next.assign(empty_reg)

        cyclix_gen.begcase(cyclix_gen.cnct(wr_r, rd_r))
        run{
            cyclix_gen.begbranch(1)  // 2'b01
            run {
                cyclix_gen.begif(cyclix_gen.bnot(empty_reg))
                run{
                    r_ptr_next.assign(r_ptr_succ)
                    full_next.assign(0)

                    credit_counter[cyclix_gen.bnot(act_counter)].assign(credit_counter[cyclix_gen.bnot(act_counter)].minus(1))

                    cyclix_gen.begif(cyclix_gen.eq2(r_ptr_succ, w_ptr_reg))
                    run{
                        empty_next.assign(1)
                    }; cyclix_gen.endif()
                }; cyclix_gen.endif()
            }; cyclix_gen.endbranch()

            cyclix_gen.begbranch(2)  // 2'b10
            run {
                cyclix_gen.begif(cyclix_gen.bnot(full_reg))
                run{
                    w_ptr_next.assign(w_ptr_succ)
                    empty_next.assign(0)
                    cyclix_gen.begif(cyclix_gen.eq2(w_ptr_succ, r_ptr_reg))
                    run{
                        full_next.assign(1)
                    }; cyclix_gen.endif()
                }; cyclix_gen.endif()
            }; cyclix_gen.endbranch()

            cyclix_gen.begbranch(3)  // 2'b11
            run {
                w_ptr_next.assign(w_ptr_succ)
                r_ptr_next.assign(r_ptr_succ)
            }; cyclix_gen.endbranch()
        }; cyclix_gen.endcase()

        full_r.assign(full_reg)
        empty_r.assign(empty_reg)

        r_data_if.assign(r_data_r)
        rd_r.assign(rd_if)

        return InputQueueCounters(rd_counter_p_r, wr_counter_p_r, r_data_if, rd_if)
    }


    fun output_queue_generation_credit_counter(
        name: String,
        data_width: Int,
        pointer_width: Int,
        depth: Int,
        tick: hw_var,
        cyclix_gen: Generic
    ): OutputQueueCounters {
        // Внешний сигнал
        val reset = cyclix_gen.uport("reset_"+name, PORT_DIR.IN, hw_dim_static(1), "0")
        val w_data_if = cyclix_gen.uglobal("w_data_if_" + name, hw_dim_static(data_width), "0")
    val wr_if = cyclix_gen.uglobal("wr_if_" + name, hw_dim_static(1), "0")
        val reset_r = cyclix_gen.uglobal("reset_r_"+name, hw_dim_static(1), "0")
//        reset.assign(reset_r)

        val rd = cyclix_gen.uport("rd_"+name, PORT_DIR.IN, hw_dim_static(1), "0")
        val rd_r = cyclix_gen.uglobal("rd_r_"+name, hw_dim_static(1), "0")
//        rd.assign(rd_r)

        val wr = cyclix_gen.uport("wr_"+name, PORT_DIR.IN, hw_dim_static(1), "0")
        val wr_r = cyclix_gen.uglobal("wr_r_"+name, hw_dim_static(1), "0")
//        wr.assign(wr_r)

        val w_data = cyclix_gen.uport("w_data_"+name, PORT_DIR.IN, hw_dim_static(data_width), "0")
        val w_data_r = cyclix_gen.uglobal("w_data_r_"+name, hw_dim_static(data_width), "0")
//        w_data.assign(w_data_r)

        val wr_counter_p = cyclix_gen.uport("wr_counter_p_"+name, PORT_DIR.OUT, hw_dim_static(8), "0")
        val wr_counter_p_r = cyclix_gen.uglobal("wr_counter_p_r_"+name, hw_dim_static(8), "0")
        wr_counter_p.assign(wr_counter_p_r)
        val rd_counter_p = cyclix_gen.uport("rd_counter_p_"+name, PORT_DIR.OUT, hw_dim_static(8), "0")
        val rd_counter_p_r = cyclix_gen.ulocal("rd_counter_p_r_"+name, hw_dim_static(8), "0")
        rd_counter_p.assign(rd_counter_p_r)

//        rd_cnt.assign(rd_counter_p_r)
//        wr_cnt.assign(wr_counter_p_r)


        val empty = cyclix_gen.uport("empty_"+name, PORT_DIR.OUT, hw_dim_static(1), "0")
        val empty_r = cyclix_gen.uglobal("empty_r_"+name, hw_dim_static(1), "0")
        empty.assign(empty_r)

        val full = cyclix_gen.uport("full_"+name, PORT_DIR.OUT, hw_dim_static(1), "0")
        val full_r = cyclix_gen.uglobal("full_r_"+name, hw_dim_static(1), "0")
        full_r.assign(full)

        val r_data = cyclix_gen.uport("r_data_"+name, PORT_DIR.OUT, hw_dim_static(data_width), "0")
        val r_data_r = cyclix_gen.uglobal("r_data_r_"+name, hw_dim_static(data_width), "0")
        r_data_r.assign(r_data)

        val array_reg_dim = hw_dim_static(data_width)
        array_reg_dim.add(depth, 0)
        var array_reg = cyclix_gen.uglobal("array_reg_"+name, array_reg_dim, "0")

        val w_ptr_reg = cyclix_gen.uglobal("w_ptr_reg_"+name, hw_dim_static(data_width-1), "0")
        val w_ptr_next = cyclix_gen.uglobal("w_ptr_next_"+name, hw_dim_static(data_width-1), "0")
        val w_ptr_succ = cyclix_gen.uglobal("w_ptr_succ_"+name, hw_dim_static(data_width-1), "0")

        val r_ptr_reg = cyclix_gen.uglobal("r_ptr_reg_"+name, hw_dim_static(data_width-1), "0")
        val r_ptr_next = cyclix_gen.uglobal("r_ptr_next_"+name, hw_dim_static(data_width-1), "0")
        val r_ptr_succ = cyclix_gen.uglobal("r_ptr_succ_"+name, hw_dim_static(data_width-1), "0")

        val full_reg = cyclix_gen.uglobal("full_reg_"+name, hw_dim_static(1), "0")
        val empty_reg = cyclix_gen.uglobal("empty_reg_"+name, hw_dim_static(1), "1")
        val full_next = cyclix_gen.uglobal("full_next_"+name, hw_dim_static(1), "0")
        val empty_next = cyclix_gen.uglobal("empty_next_"+name, hw_dim_static(1), "0")

        val wr_en = cyclix_gen.uglobal("wr_en_"+name, hw_dim_static(1), "0")

        // Инициализируем счетчики выходной очереди внутри метода:
        val queue_wr_output_counter = cyclix_gen.uglobal("queue_wr_output_counter_"+name, hw_dim_static(8), "0")
        val queue_rd_output_counter = cyclix_gen.uglobal("queue_rd_output_counter_"+name, hw_dim_static(8), "0")

        // Передаем внутренняя счетчика в наши новые переменные:
        queue_rd_output_counter.assign(rd_counter_p_r)
        queue_wr_output_counter.assign(wr_counter_p_r)

        val credit_counter_dim = hw_dim_static(8)
        credit_counter_dim.add(1,0)
        val credit_counter = cyclix_gen.uglobal("credit_counter_"+name, credit_counter_dim, "0")

        val act_counter = cyclix_gen.uglobal("act_counter"+name, hw_dim_static(0, 0), "0")

        cyclix_gen.begif(cyclix_gen.eq2(reset_r, 1))
        run{
            act_counter.assign(0)
        }; cyclix_gen.endif()

        cyclix_gen.begif(cyclix_gen.eq2(tick, 1))
        run{
            act_counter.assign(cyclix_gen.bnot(act_counter))
        }; cyclix_gen.endif()


        cyclix_gen.begif(cyclix_gen.eq2(wr_en, 1))
        run{
            array_reg[w_ptr_reg].assign(w_data_r)
            credit_counter[act_counter].assign(credit_counter[act_counter].plus(1))
        }; cyclix_gen.endif()

        wr_counter_p_r.assign(credit_counter[act_counter])
        rd_counter_p_r.assign(credit_counter[cyclix_gen.bnot(act_counter)])

        r_data_r.assign(array_reg[r_ptr_reg])

        wr_en.assign(cyclix_gen.land(wr_r, cyclix_gen.bnot(full_reg)))

        cyclix_gen.begif(cyclix_gen.eq2(reset_r, 1))
        run{
            w_ptr_reg.assign(0)
            r_ptr_reg.assign(0)
            full_reg.assign(0)
            empty_reg.assign(1)
        }; cyclix_gen.endif()
        cyclix_gen.begelse()
        run{
            w_ptr_reg.assign(w_ptr_next)
            r_ptr_reg.assign(r_ptr_next)
            full_reg.assign(full_next)
            empty_reg.assign(empty_next)
        }; cyclix_gen.endif()

        w_ptr_succ.assign(w_ptr_reg.plus(1))
        r_ptr_succ.assign(r_ptr_reg.plus(1))
        w_ptr_next.assign(w_ptr_reg)
        r_ptr_next.assign(r_ptr_reg)
        full_next.assign(full_reg)
        empty_next.assign(empty_reg)

        cyclix_gen.begcase(cyclix_gen.cnct(wr_r, rd_r))
        run{
            cyclix_gen.begbranch(1)  // 2'b01
            run {
                cyclix_gen.begif(cyclix_gen.bnot(empty_reg))
                run{
                    r_ptr_next.assign(r_ptr_succ)
                    full_next.assign(0)

                    credit_counter[cyclix_gen.bnot(act_counter)].assign(credit_counter[cyclix_gen.bnot(act_counter)].minus(1))

                    cyclix_gen.begif(cyclix_gen.eq2(r_ptr_succ, w_ptr_reg))
                    run{
                        empty_next.assign(1)
                    }; cyclix_gen.endif()
                }; cyclix_gen.endif()
            }; cyclix_gen.endbranch()

            cyclix_gen.begbranch(2)  // 2'b10
            run {
                cyclix_gen.begif(cyclix_gen.bnot(full_reg))
                run{
                    w_ptr_next.assign(w_ptr_succ)
                    empty_next.assign(0)
                    cyclix_gen.begif(cyclix_gen.eq2(w_ptr_succ, r_ptr_reg))
                    run{
                        full_next.assign(1)
                    }; cyclix_gen.endif()
                }; cyclix_gen.endif()
            }; cyclix_gen.endbranch()

            cyclix_gen.begbranch(3)  // 2'b11
            run {
                w_ptr_next.assign(w_ptr_succ)
                r_ptr_next.assign(r_ptr_succ)
            }; cyclix_gen.endbranch()
        }; cyclix_gen.endcase()

        full_r.assign(full_reg)
        empty_r.assign(empty_reg)

        w_data_r.assign(w_data_if)
        wr_r.assign(wr_if)

        return OutputQueueCounters(queue_wr_output_counter, queue_rd_output_counter, w_data_if, wr_if)

    }

    internal fun internal_spike_buffer_generation_credit_counter(
        name: String,
        data_width: Int,
        pointer_width: Int,
        depth: Int,
        tick: hw_var,

        cyclix_gen: Generic
    ): InternalSpikeBufferCounters {
        // Внешний сигнал
        val reset_r = cyclix_gen.uglobal("reset_r_"+name, hw_dim_static(1), "0")
        val rd_r = cyclix_gen.uglobal("rd_r_"+name, hw_dim_static(1), "0")
        val wr_r = cyclix_gen.uglobal("wr_r_"+name, hw_dim_static(1), "0")
        val w_data_r = cyclix_gen.uglobal("w_data_r_"+name, hw_dim_static(data_width), "0")

        val w_data_if = cyclix_gen.uglobal("w_data_" + name, hw_dim_static(data_width), "0")
        val wr_if = cyclix_gen.uglobal("wr_" + name, hw_dim_static(1), "0")
        val r_data_if = cyclix_gen.uglobal("r_data_" + name, hw_dim_static(data_width), "0")
        val rd_if = cyclix_gen.uglobal("rd_" + name, hw_dim_static(1), "0")


        val wr_counter_p = cyclix_gen.uport("wr_counter_p_"+name, PORT_DIR.OUT, hw_dim_static(8), "0")
        val wr_counter_p_r = cyclix_gen.uglobal("wr_counter_p_r_"+name, hw_dim_static(8), "0")
        wr_counter_p.assign(wr_counter_p_r)
        val rd_counter_p = cyclix_gen.uport("rd_counter_p_"+name, PORT_DIR.OUT, hw_dim_static(8), "0")
        val rd_counter_p_r = cyclix_gen.ulocal("rd_counter_p_r_"+name, hw_dim_static(8), "0")
        rd_counter_p.assign(rd_counter_p_r)

        val internal_rd_counter = cyclix_gen.uglobal("internal_rd_counter_" + name, hw_dim_static(8), "0")
        val internal_wr_counter = cyclix_gen.uglobal("internal_wr_counter_" + name, hw_dim_static(8), "0")
        internal_rd_counter.assign(rd_counter_p_r)
        internal_wr_counter.assign(wr_counter_p_r)

//        rd_cnt.assign(rd_counter_p_r)
//        wr_cnt.assign(wr_counter_p_r)

        val empty = cyclix_gen.uport("empty_"+name, PORT_DIR.OUT, hw_dim_static(1), "0")
        val empty_r = cyclix_gen.uglobal("empty_r_"+name, hw_dim_static(1), "0")
        empty.assign(empty_r)

        val full = cyclix_gen.uport("full_"+name, PORT_DIR.OUT, hw_dim_static(1), "0")
        val full_r = cyclix_gen.uglobal("full_r_"+name, hw_dim_static(1), "0")
        full_r.assign(full)

        val r_data = cyclix_gen.uport("r_data_"+name, PORT_DIR.OUT, hw_dim_static(data_width), "0")
        val r_data_r = cyclix_gen.uglobal("r_data_r_"+name, hw_dim_static(data_width), "0")
        r_data_r.assign(r_data)

        val array_reg_dim = hw_dim_static(data_width)
        array_reg_dim.add(depth, 0)
        var array_reg = cyclix_gen.uglobal("array_reg_"+name, array_reg_dim, "0")

        val w_ptr_reg = cyclix_gen.uglobal("w_ptr_reg_"+name, hw_dim_static(data_width-1), "0")
        val w_ptr_next = cyclix_gen.uglobal("w_ptr_next_"+name, hw_dim_static(data_width-1), "0")
        val w_ptr_succ = cyclix_gen.uglobal("w_ptr_succ_"+name, hw_dim_static(data_width-1), "0")

        val r_ptr_reg = cyclix_gen.uglobal("r_ptr_reg_"+name, hw_dim_static(data_width-1), "0")
        val r_ptr_next = cyclix_gen.uglobal("r_ptr_next_"+name, hw_dim_static(data_width-1), "0")
        val r_ptr_succ = cyclix_gen.uglobal("r_ptr_succ_"+name, hw_dim_static(data_width-1), "0")

        val full_reg = cyclix_gen.uglobal("full_reg_"+name, hw_dim_static(1), "0")
        val empty_reg = cyclix_gen.uglobal("empty_reg_"+name, hw_dim_static(1), "1")
        val full_next = cyclix_gen.uglobal("full_next_"+name, hw_dim_static(1), "0")
        val empty_next = cyclix_gen.uglobal("empty_next_"+name, hw_dim_static(1), "0")

        val wr_en = cyclix_gen.uglobal("wr_en_"+name, hw_dim_static(1), "0")

        val credit_counter_dim = hw_dim_static(8)
        credit_counter_dim.add(1,0)
        val credit_counter = cyclix_gen.uglobal("credit_counter_"+name, credit_counter_dim, "0")

        val act_counter = cyclix_gen.uglobal("act_counter"+name, hw_dim_static(0, 0), "0")

        cyclix_gen.begif(cyclix_gen.eq2(reset_r, 1))
        run{
            act_counter.assign(0)
        }; cyclix_gen.endif()

        cyclix_gen.begif(cyclix_gen.eq2(tick, 1))
        run{
            act_counter.assign(cyclix_gen.bnot(act_counter))
        }; cyclix_gen.endif()

        cyclix_gen.begif(cyclix_gen.eq2(wr_en, 1))
        run{
            array_reg[w_ptr_reg].assign(w_data_r)
            credit_counter[act_counter].assign(credit_counter[act_counter].plus(1))
        }; cyclix_gen.endif()

        wr_counter_p_r.assign(credit_counter[act_counter])
        rd_counter_p_r.assign(credit_counter[cyclix_gen.bnot(act_counter)])

        r_data_r.assign(array_reg[r_ptr_reg])

        wr_en.assign(cyclix_gen.land(wr_r, cyclix_gen.bnot(full_reg)))

        cyclix_gen.begif(cyclix_gen.eq2(reset_r, 1))
        run{
            w_ptr_reg.assign(0)
            r_ptr_reg.assign(0)
            full_reg.assign(0)
            empty_reg.assign(1)
        }; cyclix_gen.endif()
        cyclix_gen.begelse()
        run{
            w_ptr_reg.assign(w_ptr_next)
            r_ptr_reg.assign(r_ptr_next)
            full_reg.assign(full_next)
            empty_reg.assign(empty_next)
        }; cyclix_gen.endif()

        w_ptr_succ.assign(w_ptr_reg.plus(1))
        r_ptr_succ.assign(r_ptr_reg.plus(1))
        w_ptr_next.assign(w_ptr_reg)
        r_ptr_next.assign(r_ptr_reg)
        full_next.assign(full_reg)
        empty_next.assign(empty_reg)

        cyclix_gen.begcase(cyclix_gen.cnct(wr_r, rd_r))
        run{
            cyclix_gen.begbranch(1)  // 2'b01
            run {
                cyclix_gen.begif(cyclix_gen.bnot(empty_reg))
                run{
                    r_ptr_next.assign(r_ptr_succ)
                    full_next.assign(0)

                    credit_counter[cyclix_gen.bnot(act_counter)].assign(credit_counter[cyclix_gen.bnot(act_counter)].minus(1))

                    cyclix_gen.begif(cyclix_gen.eq2(r_ptr_succ, w_ptr_reg))
                    run{
                        empty_next.assign(1)
                    }; cyclix_gen.endif()
                }; cyclix_gen.endif()
            }; cyclix_gen.endbranch()

            cyclix_gen.begbranch(2)  // 2'b10
            run {
                cyclix_gen.begif(cyclix_gen.bnot(full_reg))
                run{
                    w_ptr_next.assign(w_ptr_succ)
                    empty_next.assign(0)
                    cyclix_gen.begif(cyclix_gen.eq2(w_ptr_succ, r_ptr_reg))
                    run{
                        full_next.assign(1)
                    }; cyclix_gen.endif()
                }; cyclix_gen.endif()
            }; cyclix_gen.endbranch()

            cyclix_gen.begbranch(3)  // 2'b10
            run {
                w_ptr_next.assign(w_ptr_succ)
                r_ptr_next.assign(r_ptr_succ)
            }; cyclix_gen.endbranch()
        }; cyclix_gen.endcase()

        full_r.assign(full_reg)
        empty_r.assign(empty_reg)

        w_data_r.assign(w_data_if)
        wr_r.assign(wr_if)

        r_data_if.assign(r_data_r)
        rd_r.assign(rd_if)

        return InternalSpikeBufferCounters(internal_rd_counter, internal_wr_counter, w_data_if, wr_if, r_data_if, rd_if)
    }
}


class StaticParams(
    val name: String,
    val bitWidth: Int,
    val presynNeurons: Int,
    val postsynNeurons: Int
) {
    fun generate(cyclix_gen: Generic): hw_var {
        val memDim = hw_dim_static(bitWidth)
        memDim.add(presynNeurons - 1, 0)
        memDim.add(postsynNeurons - 1, 0)
        return cyclix_gen.sglobal(name, memDim, "0")
    }
}

class DynamicParam(
    val name: String,
    val bitWidth: Int,
    val postsynNeurons: Int
) {
    fun generate(cyclix_gen: Generic): hw_var {
        val memDim = hw_dim_static(bitWidth)
        memDim.add(postsynNeurons - 1, 0)
        return cyclix_gen.uglobal(name, memDim, "0")
    }
}

enum class NEURAL_NETWORK_TYPE {
    SFNN, SCNN
}

val OP_SOMATIC_PHASE = hwast.hw_opcode("somatic_phase")
val OP_SYNAPTIC_PHASE = hwast.hw_opcode("synaptic_phase")

//val OP_SYN_ACC = hwast.hw_opcode("synaptic_acc")
//
//val OP_NEURONS_ACC = hwast.hw_opcode("neuronal_acc")
//val OP_SYN_ASSIGN = hwast.hw_opcode("syn_assign")

val OP_NEURON_MINUS = hwast.hw_opcode("neuron_minus")
val OP_NEURON_RIGHT_SHIFT = hwast.hw_opcode("neuron_right_shift")
val OP_NEURON_COMPARE = hwast.hw_opcode("neuron_compare")
val OP_NEURON_HANDLER = hwast.hw_opcode("neuron_handler")
val OP_EMISSION_SPK = hwast.hw_opcode("spike_emission")

val OP_SYN_PLUS = hwast.hw_opcode("syn_plus")


class neuro_local(name : String, vartype : hw_type, defimm : hw_imm)
    : hw_var(name, vartype, defimm) {

    constructor(name : String, vartype : hw_type, defval : String)
            : this(name, vartype, hw_imm(defval))
}

class neuro_global(name : String, vartype : hw_type, defimm : hw_imm)
    : hw_var(name, vartype, defimm) {

    constructor(name : String, vartype : hw_type, defval : String)
            : this(name, vartype, hw_imm(defval))
}

class hw_var_soma(name: String, vartype: hw_type, defimm: hw_imm) : hw_var(name, vartype, defimm) {
    val op2Expressions = mutableListOf<hw_var>()
    val op2Src0 = mutableListOf<hw_param>()
    val op2Src1 = mutableListOf<Int>()

    fun AddExpr_op2(opcode: hw_opcode, src0: hw_param, src1: Int): hw_var {
        val newExpr = hw_var(name, DATA_TYPE.BV_UNSIGNED, 0, 0, "0")
        op2Expressions.add(newExpr)
        op2Src0.add(src0)
        op2Src1.add(src1)

        return newExpr
    }

    fun geq(src0: hw_var_soma, src1: Int): hw_var {
        return AddExpr_op2(OP2_LOGICAL_GEQ, src0, src1)
    }
}

open class hw_somatic_phase(val name: String, val neuromorphic: Neuromorphic): hw_exec(OP_SOMATIC_PHASE) {
    // Внутренние списки для хранения операндов (destination и source)
    val dst = mutableListOf<hw_var>()
    val src = mutableListOf<Int>()
    val operations = mutableListOf<hw_exec>()

    fun srl(op1: hw_var, op2: Int) {
        dst.add(op1)
        src.add(op2)
        val shiftOp = hw_exec(OP_NEURON_RIGHT_SHIFT)
        operations.add(shiftOp)
    }

    fun single_spike() {
        val spikeOp = hw_exec(OP_EMISSION_SPK)
        operations.add(spikeOp)
    }


    fun begif(cond: hw_param) {

        val new_expr = hw_exec(OP1_IF)

        if (cond is hw_var) {
            val genvar = hw_var("gen_var", DATA_TYPE.BV_UNSIGNED, 0, 0, "0")
            new_expr.AddGenVar(genvar)
//            assign_gen(genvar, cond)
            new_expr.AddParam(genvar)
//            last().priority_conditions.add(genvar)
        }

    }

    fun begin() {
        neuromorphic.begproc_soma(this)
    }
}

open class hw_synaptic_phase(val name: String, val neuromorphic: Neuromorphic): hw_exec(OP_SYNAPTIC_PHASE) {
    val dst = mutableListOf<hw_var>()
    val src = mutableListOf<hw_var>()

    val operations = mutableListOf<hw_exec>()

    fun plus(membrane_potential: hw_var, weight: hw_var, nnType: NEURAL_NETWORK_TYPE) {
        dst.add(membrane_potential)
        src.add(weight)

        val plusOp = when (nnType) {
            NEURAL_NETWORK_TYPE.SFNN -> hw_exec(OP_SYN_PLUS)
            NEURAL_NETWORK_TYPE.SCNN -> hw_exec(OP_SYN_PLUS)
            else -> hw_exec(OP_SYN_PLUS)
        }

        operations.add(plusOp)
    }

    fun begin() {
        neuromorphic.begproc_syna(this)
    }
}

open class SynapticTr(val name: String) {
    val fields = mutableListOf<hw_var>()

    fun add_field(fieldName: String, width: Int): hw_var {
//        val newField = cyclix_gen.uglobal("${name}_${fieldName}", hw_dim_static(width), "0")
        val newField = hw_var("${name}_${fieldName}", DATA_TYPE.BV_UNSIGNED,  "0")
        fields.add(newField)
        return newField
    }
}

class SomaticTr(val name: String) {
    val fields = mutableListOf<hw_var_soma>()

    fun add_field(fieldName: String, width: Int): hw_var_soma {
//        val newField = cyclix_gen.uglobal("${name}_${fieldName}", hw_dim_static(width), "0")
        val newField = hw_var_soma("${name}_${fieldName}", hw_type(DATA_TYPE.BV_UNSIGNED, "0"),  hw_imm("0"))
        fields.add(newField)
        return newField
    }
}

open class SnnArch(
    var name: String = "Default Name",
    var nnType: NEURAL_NETWORK_TYPE = NEURAL_NETWORK_TYPE.SFNN,
    var presyn_neurons: Int = 10,
    var postsyn_neurons: Int = 10,
    var outputNeur: Int = 10,
    var weightWidth: Int = 10,
    var potentialWidth: Int = 10,
    var leakage: Int = 1,
    var threshold: Int = 1,
    var reset: Int = 0
) {
    fun loadModelFromJson(jsonFilePath: String) {
        val jsonString = File(jsonFilePath).readText()

        val jsonObject = JSONObject(jsonString)

        val modelTopology = jsonObject.getJSONObject("model_topology")
        this.presyn_neurons = modelTopology.optInt("input_size", this.presyn_neurons)
        this.postsyn_neurons = modelTopology.optInt("hidden_size", this.postsyn_neurons)
        this.outputNeur = modelTopology.optInt("output_size", this.outputNeur)
        val lifNeurons = jsonObject.getJSONObject("LIF_neurons").getJSONObject("lif1")
        this.threshold = lifNeurons.optInt("threshold", this.threshold)
        this.leakage = lifNeurons.optInt("leakage", this.leakage)

        val nnTypeStr = jsonObject.optString("nn_type", "SFNN")
        this.nnType = NEURAL_NETWORK_TYPE.valueOf(nnTypeStr)
    }

    fun getArchitectureInfo(): String {
        return "$name: (NN Type: $nnType, Presynaptic Neurons = $presyn_neurons, Postsynaptic Neurons = $postsyn_neurons)"
    }
}

open class Neuromorphic(val name : String, val NN_model_params: SnnArch) : hw_astc_stdif() {

    var locals = ArrayList<neuro_local>()
    var globals = ArrayList<neuro_global>()

    private fun add_local(new_local: neuro_local) {
        if (wrvars.containsKey(new_local.name)) ERROR("Naming conflict for local: " + new_local.name)
        if (rdvars.containsKey(new_local.name)) ERROR("Naming conflict for local: " + new_local.name)

        wrvars.put(new_local.name, new_local)
        rdvars.put(new_local.name, new_local)
        locals.add(new_local)
        new_local.default_astc = this
    }

    private fun add_global(new_global: neuro_global) {
        if (wrvars.containsKey(new_global.name)) ERROR("Naming conflict for global: " + new_global.name)
        if (rdvars.containsKey(new_global.name)) ERROR("Naming conflict for global: " + new_global.name)

        wrvars.put(new_global.name, new_global)
        rdvars.put(new_global.name, new_global)
        globals.add(new_global)
        new_global.default_astc = this
    }

    fun local(name: String, vartype: hw_type, defimm: hw_imm): neuro_local {
        var ret_var = neuro_local(name, vartype, defimm)
        add_local(ret_var)
        return ret_var
    }

    fun local(name: String, src_struct: hw_struct, dimensions: hw_dim_static): neuro_local {
        var ret_var = neuro_local(name, hw_type(src_struct, dimensions), "0")
        add_local(ret_var)
        return ret_var
    }

    fun local(name: String, src_struct: hw_struct): neuro_local {
        var ret_var = neuro_local(name, hw_type(src_struct), "0")
        add_local(ret_var)
        return ret_var
    }

    fun ulocal(name: String, dimensions: hw_dim_static, defimm: hw_imm): neuro_local {
        var ret_var = neuro_local(name, hw_type(DATA_TYPE.BV_UNSIGNED, dimensions), defimm)
        add_local(ret_var)
        return ret_var
    }

    fun ulocal(name: String, dimensions: hw_dim_static, defval: String): neuro_local {
        var ret_var = neuro_local(name, hw_type(DATA_TYPE.BV_UNSIGNED, dimensions), defval)
        add_local(ret_var)
        return ret_var
    }

    fun ulocal(name: String, msb: Int, lsb: Int, defimm: hw_imm): neuro_local {
        var ret_var = neuro_local(name, hw_type(DATA_TYPE.BV_UNSIGNED, msb, lsb), defimm)
        add_local(ret_var)
        return ret_var
    }

    fun ulocal(name: String, msb: Int, lsb: Int, defval: String): neuro_local {
        var ret_var = neuro_local(name, hw_type(DATA_TYPE.BV_UNSIGNED, msb, lsb), defval)
        add_local(ret_var)
        return ret_var
    }

    fun ulocal(name: String, defimm: hw_imm): neuro_local {
        var ret_var = neuro_local(name, hw_type(DATA_TYPE.BV_UNSIGNED, defimm.imm_value), defimm)
        add_local(ret_var)
        return ret_var
    }

    fun ulocal(name: String, defval: String): neuro_local {
        var ret_var = neuro_local(name, hw_type(DATA_TYPE.BV_UNSIGNED, defval), defval)
        add_local(ret_var)
        return ret_var
    }

    fun slocal(name: String, dimensions: hw_dim_static, defimm: hw_imm): neuro_local {
        var ret_var = neuro_local(name, hw_type(DATA_TYPE.BV_SIGNED, dimensions), defimm)
        add_local(ret_var)
        return ret_var
    }

    fun slocal(name: String, dimensions: hw_dim_static, defval: String): neuro_local {
        var ret_var = neuro_local(name, hw_type(DATA_TYPE.BV_SIGNED, dimensions), defval)
        add_local(ret_var)
        return ret_var
    }

    fun slocal(name: String, msb: Int, lsb: Int, defimm: hw_imm): neuro_local {
        var ret_var = neuro_local(name, hw_type(DATA_TYPE.BV_SIGNED, msb, lsb), defimm)
        add_local(ret_var)
        return ret_var
    }

    fun slocal(name: String, msb: Int, lsb: Int, defval: String): neuro_local {
        var ret_var = neuro_local(name, hw_type(DATA_TYPE.BV_SIGNED, msb, lsb), defval)
        add_local(ret_var)
        return ret_var
    }

    fun slocal(name: String, defimm: hw_imm): neuro_local {
        var ret_var = neuro_local(name, hw_type(DATA_TYPE.BV_SIGNED, defimm.imm_value), defimm)
        add_local(ret_var)
        return ret_var
    }

    fun slocal(name: String, defval: String): neuro_local {
        var ret_var = neuro_local(name, hw_type(DATA_TYPE.BV_SIGNED, defval), defval)
        add_local(ret_var)
        return ret_var
    }


    fun global(name: String, vartype: hw_type, defimm: hw_imm): neuro_global {
        var ret_var = neuro_global(name, vartype, defimm)
        add_global(ret_var)
        return ret_var
    }

    fun global(name: String, vartype: hw_type, defval: String): neuro_global {
        var ret_var = neuro_global(name, vartype, defval)
        add_global(ret_var)
        return ret_var
    }

    fun global(name: String, src_struct: hw_struct, dimensions: hw_dim_static): neuro_global {
        var ret_var = neuro_global(name, hw_type(src_struct, dimensions), "0")
        add_global(ret_var)
        return ret_var
    }

    fun global(name: String, src_struct: hw_struct): neuro_global {
        var ret_var = neuro_global(name, hw_type(src_struct), "0")
        add_global(ret_var)
        return ret_var
    }

    fun uglobal(name: String, dimensions: hw_dim_static, defimm: hw_imm): neuro_global {
        var ret_var = neuro_global(name, hw_type(DATA_TYPE.BV_UNSIGNED, dimensions), defimm)
        add_global(ret_var)
        return ret_var
    }

    fun uglobal(name: String, dimensions: hw_dim_static, defval: String): neuro_global {
        var ret_var = neuro_global(name, hw_type(DATA_TYPE.BV_UNSIGNED, dimensions), defval)
        add_global(ret_var)
        return ret_var
    }

    fun uglobal(name: String, msb: Int, lsb: Int, defimm: hw_imm): neuro_global {
        var ret_var = neuro_global(name, hw_type(DATA_TYPE.BV_UNSIGNED, msb, lsb), defimm)
        add_global(ret_var)
        return ret_var
    }

    fun uglobal(name: String, msb: Int, lsb: Int, defval: String): neuro_global {
        var ret_var = neuro_global(name, hw_type(DATA_TYPE.BV_UNSIGNED, msb, lsb), defval)
        add_global(ret_var)
        return ret_var
    }

    fun uglobal(name: String, defimm: hw_imm): neuro_global {
        var ret_var = neuro_global(name, hw_type(DATA_TYPE.BV_UNSIGNED, defimm.imm_value), defimm)
        add_global(ret_var)
        return ret_var
    }

    fun uglobal(name: String, defval: String): neuro_global {
        var ret_var = neuro_global(name, hw_type(DATA_TYPE.BV_UNSIGNED, defval), defval)
        add_global(ret_var)
        return ret_var
    }

    fun sglobal(name: String, dimensions: hw_dim_static, defimm: hw_imm): neuro_global {
        var ret_var = neuro_global(name, hw_type(DATA_TYPE.BV_SIGNED, dimensions), defimm)
        add_global(ret_var)
        return ret_var
    }

    fun sglobal(name: String, dimensions: hw_dim_static, defval: String): neuro_global {
        var ret_var = neuro_global(name, hw_type(DATA_TYPE.BV_SIGNED, dimensions), defval)
        add_global(ret_var)
        return ret_var
    }

    fun sglobal(name: String, msb: Int, lsb: Int, defimm: hw_imm): neuro_global {
        var ret_var = neuro_global(name, hw_type(DATA_TYPE.BV_SIGNED, msb, lsb), defimm)
        add_global(ret_var)
        return ret_var
    }

    fun sglobal(name: String, msb: Int, lsb: Int, defval: String): neuro_global {
        var ret_var = neuro_global(name, hw_type(DATA_TYPE.BV_SIGNED, msb, lsb), defval)
        add_global(ret_var)
        return ret_var
    }

    fun sglobal(name: String, defimm: hw_imm): neuro_global {
        var ret_var = neuro_global(name, hw_type(DATA_TYPE.BV_SIGNED, defimm.imm_value), defimm)
        add_global(ret_var)
        return ret_var
    }

    fun sglobal(name: String, defval: String): neuro_global {
        var ret_var = neuro_global(name, hw_type(DATA_TYPE.BV_SIGNED, defval), defval)
        add_global(ret_var)
        return ret_var
    }

    fun somatic_phase(name: String, postsyn_neurons: Int ) : hw_somatic_phase {
        var new_phase = hw_somatic_phase(name, this)
        return new_phase
    }

    fun synaptic_phase(name: String, presyn_neurons: Int, postsyn_neurons: Int ) : hw_synaptic_phase {
        var new_phase = hw_synaptic_phase(name, this)
        return new_phase
    }

    fun begproc_soma(neurohandler: hw_somatic_phase) {
        this.add(neurohandler)
    }

    fun begproc_syna(neurohandler: hw_synaptic_phase) {
        this.add(neurohandler)
    }



    fun translate(debug_lvl : DEBUG_LEVEL) : cyclix.Generic {
        var cyclix_gen = cyclix.Generic(name)

        val presyn_neurons = 128
        val postsyn_neurons = 128
        val weight_width = 4
        val potential_width = 7
        val threshold = 1
        val leak = 1
        val reset_voltage = 0
        MSG(""+presyn_neurons+postsyn_neurons+weight_width+potential_width)

        //  generation tick
        var tick =  cyclix_gen.uglobal("tick", "0")
        val tickGen = TickGenerator()
        tickGen.tick_generation(tick, 100, "ns", 10, cyclix_gen)



        val queues = Queues()

// Инициализация входящей очереди и получение структуры с интерфейсными сигналами и каунтерами
        val inputQueueCounters = queues.input_queue_generation_credit_counter(
            "L1_input_queue",
            8,                  // data_width
            4,                  // pointer_width
            presyn_neurons - 1, // depth
            tick,
            cyclix_gen
        )

// Инициализация исходящей очереди и получение структуры с интерфейсными сигналами и каунтерами
        val outputQueueCounters = queues.output_queue_generation_credit_counter(
            "l1_output_queue",
            8,                  // data_width
            4,                  // pointer_width
            postsyn_neurons - 1, // depth
            tick,
            cyclix_gen
        )


        // Память весов
        val weightMemory = StaticParams("l1_weights_mem", weight_width, presyn_neurons, postsyn_neurons)
        var l1_weight_memory = weightMemory.generate(cyclix_gen)

        // Память статических параметров
        val dynamicParam = DynamicParam("l1_membrane_potential_memory", potential_width, postsyn_neurons)
        var l1_membrane_potential_memory = dynamicParam.generate(cyclix_gen)



        // Контроллер
        val en_core = cyclix_gen.uport("en_core", PORT_DIR.IN, hw_dim_static(1), "0")
        val reg_en_core = cyclix_gen.uglobal("reg_en_core", hw_dim_static(1), "0")
        reg_en_core.assign(en_core) // todo: заменить в сгенерированном верилоге

        // Состояния контроллера для одного слоя
        val STATE_IDLE = 0
        val STATE_READ_IN_SPK = 1
        val STATE_SYNAPTIC = 2
        val STATE_SOMATIC = 3
        val STATE_EMISSION = 4

        // Переменная для отслеживания состояния обработки
        val current_state = cyclix_gen.uglobal("current_state", hw_dim_static(2, 0), "0")
        val next_state = cyclix_gen.uglobal("next_state", hw_dim_static(2, 0), "0")
        current_state.assign(next_state)

        val postsyn_counter = cyclix_gen.uglobal("postsyn_counter", hw_dim_static(8), "0")  // Параметризировать разрядность
        val l1_presyn_neuron_id = cyclix_gen.uglobal("l1_presyn_neuron_id", hw_dim_static(8), "0")

        val presyn_cnt = cyclix_gen.uglobal("presyn_cnt", hw_dim_static(8), "0")


//        // Память весов
//        var l1_weights_mem_ext_dim = hw_dim_static(weight_width)
//        l1_weights_mem_dim.add(postsyn_neurons-1, 0)  // 256
//        var l1_weights_mem_ext = cyclix_gen.sglobal("l1_weights_mem_ext", l1_weights_mem_ext_dim, "0")
//
        // Логика контроллера
        cyclix_gen.begif(cyclix_gen.eq2(reg_en_core, 1))  // Если старт обработки активен
        run {
            cyclix_gen.begif(cyclix_gen.eq2(current_state, STATE_IDLE))  // Состояние "идл"
            run {
                inputQueueCounters.rd_if.assign(0)
                presyn_cnt.assign(0)
                l1_presyn_neuron_id.assign(0)
                postsyn_counter.assign(0)

                next_state.assign(STATE_READ_IN_SPK)
            }; cyclix_gen.endif()

            cyclix_gen.begif(cyclix_gen.eq2(current_state, STATE_READ_IN_SPK))
            run {
                cyclix_gen.begif(cyclix_gen.eq2(tick, 1))
                run {
                    inputQueueCounters.rd_if.assign(1)
                    next_state.assign(STATE_SYNAPTIC)
                }; cyclix_gen.endif()
            }; cyclix_gen.endif()

            // Обработка пресинаптического счётчика
            cyclix_gen.begif(cyclix_gen.eq2(current_state, STATE_SYNAPTIC))
            run {
//                l1_rd_sig.assign(1)
//                l1_presyn_neuron_id.assign(l1_src_neuron_id)
                cyclix_gen.begif(cyclix_gen.neq2(inputQueueCounters.rdCounter, presyn_cnt))
                run {
                    for (i in 0..postsyn_neurons - 1) {
                        //                l1_weights_mem_ext.assign(l1_weight_memory[l1_presyn_neuron_id][i])
                        l1_membrane_potential_memory[i].assign(l1_membrane_potential_memory[i].plus(l1_weight_memory[inputQueueCounters.r_data_if][i]))
                    }
                };cyclix_gen.endif()
//            }; cyclix_gen.endif()
                cyclix_gen.begelse()
                run {
                   // l1_rd_sig.assign(0)
                    next_state.assign(STATE_SOMATIC)
                }; cyclix_gen.endif()

            }; cyclix_gen.endif()

            cyclix_gen.begif(cyclix_gen.eq2(current_state, STATE_SOMATIC))
            run {
                for (i in 0..postsyn_neurons - 1) {
                    //                l1_weights_mem_ext.assign(l1_weight_memory[l1_presyn_neuron_id][i])
                    l1_membrane_potential_memory[i].assign(cyclix_gen.srl(l1_membrane_potential_memory[i], leak))
//                    l1_membrane_potential_memory[i].assign(l1_membrane_potential_memory[i].minus(1))
                }

                postsyn_counter.assign(0)
//                    next_state.assign(STATE_EMISSION)
                next_state.assign(STATE_EMISSION)
            }; cyclix_gen.endif()

            cyclix_gen.begif(cyclix_gen.eq2(current_state, STATE_EMISSION))
            run {

                cyclix_gen.begif(cyclix_gen.less(postsyn_counter, postsyn_neurons-1))
                run {
                    cyclix_gen.begif(cyclix_gen.geq(l1_membrane_potential_memory[postsyn_counter], threshold))
                    run {
//                        l1_wr_sig.assign(1)
//                        l1_trg_neuron_id.assign(postsyn_counter)
                        outputQueueCounters.wr_if.assign(1)
                        outputQueueCounters.w_data_if.assign(postsyn_counter)
                        l1_membrane_potential_memory[postsyn_counter].assign(reset_voltage)
                    }; cyclix_gen.endif()
                    cyclix_gen.begelse()
                    run{
                        outputQueueCounters.wr_if.assign(0)
                    };cyclix_gen.endif()

                    postsyn_counter.assign(postsyn_counter.plus(1))
                }; cyclix_gen.endif()

                cyclix_gen.begif(cyclix_gen.eq2(postsyn_counter, postsyn_neurons-1))
                run{
                    postsyn_counter.assign(0)
                    next_state.assign(STATE_IDLE)
                };cyclix_gen.endif()
            }; cyclix_gen.endif() // cyclix_gen.begif(cyclix_gen.eq2(current_state, STATE_EMISSION))

            }; cyclix_gen.endif()

        return cyclix_gen
    }
}

fun main(){

}
