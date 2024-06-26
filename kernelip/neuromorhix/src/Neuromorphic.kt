/*
 * Neuromorphic.kt
 *     License: See LICENSE file for details
 */

package neuromorphix

import cyclix.*
import hwast.*
import kotlin.math.ln
import javax.sound.sampled.Port

enum class NEURAL_NETWORK_TYPE {
    SFNN, SCNN
    // add memristor based
}

val OP_NEURO = hwast.hw_opcode("neuro_proc")


data class SCHEDULER_BUF_SIZE(val SIZE : Int)

data class STATIC_MEMORY_SIZE(val width : Int, val depth : Int)  // N*M matrix of weight[width:0]

data class DYNAMIC_MEMORY_SIZE(val width : Int, val depth : Int)

open class snn_arch(val name : String, val nn_type : NEURAL_NETWORK_TYPE, var presyn_neur : Int, val postsyn_neur : Int, val weight_width : Int, val potential_width : Int ) {

    fun getArchitectureInfo(): String {
        return "$name: " +
                "(NN Type: $nn_type, " +
                "Presynaptic Neurons = $presyn_neur, " +
                "Postsynaptic Neurons = $postsyn_neur)"
    }

    fun getPresynNeurNum() : Int {
        return presyn_neur    // this
    }

    fun getPostsynNeurNum() : Int {
        return  postsyn_neur
    }

    fun getWeightWidth() : Int {
        return  weight_width
    }

    fun getPotentialWidth() : Int {
        return  potential_width
    }

//    fun updateNeurons(presyn_neur: Int, postsyn_neur: Int) {
//        this.presyn_neur = presyn_neur
//        this.postsyn_neur = postsyn_neur
//    }

}

class sfnn_memory_models(val name: String, nn_model : snn_arch) {
    val static_memory =  STATIC_MEMORY_SIZE(nn_model.weight_width,nn_model.presyn_neur*nn_model.postsyn_neur)    // weight memory num = num of presynaptic neurons * num of postsynaptic neurons
    val dynamic_memory =  DYNAMIC_MEMORY_SIZE(nn_model.potential_width, nn_model.postsyn_neur)    // membrane potential memory num = num of postsynaptic neurons
    val scheduler_buffer_memory =  SCHEDULER_BUF_SIZE(nn_model.presyn_neur*nn_model.postsyn_neur)

    fun printMemValues() : String {
        return "static_memory: $static_memory,\n" +
                "dynamic_memory: $dynamic_memory,\n" +
                "scheduler_buffer_memory: $scheduler_buffer_memory"
    }

}

class hw_neuro_handler(val name : String, val neuromorphic: Neuromorphic) : hwast.hw_exec(OP_NEURO) {

    fun begin() {
        neuromorphic.begproc(this)
    }
}

class neuro_local(name : String, vartype : hw_type, defimm : hw_imm)    // переменная существующая между тактами
    : hw_var(name, vartype, defimm) {

    constructor(name : String, vartype : hw_type, defval : String)
            : this(name, vartype, hw_imm(defval))
}

class neuro_global(name : String, vartype : hw_type, defimm : hw_imm)    // переменная существующая весь вычислительный процесс
    : hw_var(name, vartype, defimm) {

    constructor(name : String, vartype : hw_type, defval : String)
            : this(name, vartype, hw_imm(defval))
}


class neuro_epochal(name : String, vartype : hw_type, defimm : hw_imm)    // переменная существующая в пределах тика
    : hw_var(name, vartype, defimm) {

    constructor(name : String, vartype : hw_type, defval : String)
            : this(name, vartype, hw_imm(defval))
}

internal class __neurons_proc_info(cyclix_gen : cyclix.Generic,
                                   name_prefix : String,
                                   val TranslateInfo : __TranslateInfo,
                                   val tick : hw_var) {

    var nContext_epochals_dict     = mutableMapOf<hw_var, hw_var>()    // local variables

    var var_dict            = mutableMapOf<hw_var, hw_var>()
}

//internal class epochals_processing(cyclix_gen: Generic, var tick : hw_var){
//    var slot_itmelife = ArrayList<hw_var>()
//
//    init {
//        cyclix_gen.begif(tick)
//        run {
//
//        }; cyclix_gen.endif()
//
//        cyclix_gen.begelse()
//        run{
//
//        }
//    }
//}



open class i_buffer_if( name_prefix : String, TRX_BUF_SIZE : Int, data_in : hw_var, ) {
    var name = name_prefix
    var data_in = data_in
}

open class o_buffer_if( name_prefix : String, TRX_BUF_SIZE : Int, data_out : hw_var) {
    var name = name_prefix
    var data_out = data_out

}

internal class __TranslateInfo(var neuromorphic : Neuromorphic) {
    var __global_assocs = mutableMapOf<hw_var, hw_var>()
    var __local_assocs = mutableMapOf<hw_var, hw_var>()
    var __epochal_assocs = mutableMapOf<hw_var, hw_var>()
    var __processing_handlers_assocs = mutableMapOf<hw_neuro_handler, __neurons_proc_info>()  // neuron_handler
    var ProcessList = ArrayList<hw_neuro_handler>()
    var ProcessInfoList = ArrayList<__neurons_proc_info>()
    var __i_buffers = mutableMapOf<i_buffer_if, hw_fifo>()
    var __o_buffers = mutableMapOf<o_buffer_if, hw_fifo>()
    var gen_neurons_count = DUMMY_VAR
}

open class Neuromorphic(val name : String, val snn : snn_arch, val tick_slot : Int ) : hw_astc_stdif() {

    var locals          = ArrayList<neuro_local>()
    var globals         = ArrayList<neuro_global>()
    var epochals        = ArrayList<neuro_epochal>()
    var i_buffer      = ArrayList<i_buffer_if>()
    var o_buffer      = ArrayList<o_buffer_if>()

    var Procs = mutableMapOf<String, hw_neuro_handler>()


    fun neuron_handler(name : String) : hw_neuro_handler {
        var new_neuron_process = hw_neuro_handler(name, this)
        Procs.put(new_neuron_process.name, new_neuron_process)

        return new_neuron_process
    }

    fun log2(value: Int): Int {
        require(value > 0) { "Value must be greater than 0" }
        var result = 0
        var tempValue = value
        while (tempValue > 1) {
            tempValue /= 2
            result++
        }

        return result
    }

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

    private fun add_epochal(new_epochal: neuro_epochal) {
        if (wrvars.containsKey(new_epochal.name)) ERROR("Naming conflict for epochal: " + new_epochal.name)
        if (rdvars.containsKey(new_epochal.name)) ERROR("Naming conflict for epochal: " + new_epochal.name)

        wrvars.put(new_epochal.name, new_epochal)
        rdvars.put(new_epochal.name, new_epochal)
        epochals.add(new_epochal)
        new_epochal.default_astc = this
    }

    private fun add_i_buffers(new_i_buffer: i_buffer_if){
        i_buffer.add(new_i_buffer)
    }

    private fun add_o_buffers(new_o_buffer: o_buffer_if){
        o_buffer.add(new_o_buffer)
    }

    fun local(name: String, vartype : hw_type, defimm: hw_imm): neuro_local {
        var ret_var = neuro_local(name, vartype, defimm)
        add_local(ret_var)
        return ret_var
    }

    fun local(name: String, vartype : hw_type, defval: String): neuro_local {
        var ret_var = neuro_local(name, vartype, defval)
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


    fun epochal(name: String, vartype: hw_type, defimm: hw_imm): neuro_epochal {
        var ret_var = neuro_epochal(name, vartype, defimm)
        add_epochal(ret_var)
        return ret_var
    }

    fun epochal(name: String, vartype : hw_type, defval: String): neuro_epochal {
        var ret_var = neuro_epochal(name, vartype, defval)
        add_epochal(ret_var)
        return ret_var
    }

    fun epochal(name: String, src_struct: hw_struct, dimensions: hw_dim_static): neuro_epochal {
        var ret_var = neuro_epochal(name, hw_type(src_struct, dimensions), "0")
        add_epochal(ret_var)
        return ret_var
    }

    fun epochal(name: String, src_struct: hw_struct): neuro_epochal {
        var ret_var = neuro_epochal(name, hw_type(src_struct), "0")
        add_epochal(ret_var)
        return ret_var
    }

    fun uepochal(name: String, dimensions: hw_dim_static, defimm: hw_imm): neuro_epochal {
        var ret_var = neuro_epochal(name, hw_type(DATA_TYPE.BV_UNSIGNED, dimensions), defimm)
        add_epochal(ret_var)
        return ret_var
    }

    fun uepochal(name: String, dimensions: hw_dim_static, defval: String): neuro_epochal {
        var ret_var = neuro_epochal(name, hw_type(DATA_TYPE.BV_UNSIGNED, dimensions), defval)
        add_epochal(ret_var)
        return ret_var
    }

    fun uepochal(name: String, msb: Int, lsb: Int, defimm: hw_imm): neuro_epochal {
        var ret_var = neuro_epochal(name, hw_type(DATA_TYPE.BV_UNSIGNED, msb, lsb), defimm)
        add_epochal(ret_var)
        return ret_var
    }

    fun uepochal(name: String, msb: Int, lsb: Int, defval: String): neuro_epochal {
        var ret_var = neuro_epochal(name, hw_type(DATA_TYPE.BV_UNSIGNED, msb, lsb), defval)
        add_epochal(ret_var)
        return ret_var
    }

    fun uepochal(name: String, defimm: hw_imm): neuro_epochal {
        var ret_var = neuro_epochal(name, hw_type(DATA_TYPE.BV_UNSIGNED, defimm.imm_value), defimm)
        add_epochal(ret_var)
        return ret_var
    }

    fun uepochal(name: String, defval: String): neuro_epochal {
        var ret_var = neuro_epochal(name, hw_type(DATA_TYPE.BV_UNSIGNED, defval), defval)
        add_epochal(ret_var)
        return ret_var
    }

    fun sepochal(name: String, dimensions: hw_dim_static, defimm: hw_imm): neuro_epochal {
        var ret_var = neuro_epochal(name, hw_type(DATA_TYPE.BV_SIGNED, dimensions), defimm)
        add_epochal(ret_var)
        return ret_var
    }

    fun sepochal(name: String, dimensions: hw_dim_static, defval: String): neuro_epochal {
        var ret_var = neuro_epochal(name, hw_type(DATA_TYPE.BV_SIGNED, dimensions), defval)
        add_epochal(ret_var)
        return ret_var
    }

    fun sepochal(name: String, msb: Int, lsb: Int, defimm: hw_imm): neuro_epochal {
        var ret_var = neuro_epochal(name, hw_type(DATA_TYPE.BV_SIGNED, msb, lsb), defimm)
        add_epochal(ret_var)
        return ret_var
    }

    fun sepochal(name: String, msb: Int, lsb: Int, defval: String): neuro_epochal {
        var ret_var = neuro_epochal(name, hw_type(DATA_TYPE.BV_SIGNED, msb, lsb), defval)
        add_epochal(ret_var)
        return ret_var
    }

    fun sepochal(name: String, defimm: hw_imm): neuro_epochal {
        var ret_var = neuro_epochal(name, hw_type(DATA_TYPE.BV_SIGNED, defimm.imm_value), defimm)
        add_epochal(ret_var)
        return ret_var
    }

    fun sepochal(name: String, defval: String): neuro_epochal {
        var ret_var = neuro_epochal(name, hw_type(DATA_TYPE.BV_SIGNED, defval), defval)
        add_epochal(ret_var)
        return ret_var
    }


    fun i_buffer( name_prefix : String,  TRX_BUF_SIZE : Int,  TRX_BUF_MULTIDIM : Int, data_in: hw_var ) : i_buffer_if{
        var ret_buf = i_buffer_if(name_prefix, snn.presyn_neur*snn.postsyn_neur, data_in )
        add_i_buffers(ret_buf)
        return ret_buf
    }

    fun o_buffer( name_prefix : String,  TRX_BUF_SIZE : Int,  TRX_BUF_MULTIDIM : Int, data_out: hw_var ) : o_buffer_if{
        var ret_buf = o_buffer_if(name_prefix, snn.postsyn_neur, data_out )
        add_o_buffers(ret_buf)
        return ret_buf
    }

    fun begproc(neurohandler : hw_neuro_handler){
        add(neurohandler)
    }

    fun endtimeslot() {
        this.clear()
    }

    var __ports_assocs = mutableMapOf<hw_var, hw_var>()

    fun reconstruct_expression(debug_lvl : DEBUG_LEVEL,
                               cyclix_gen : hw_astc,
                               expr : hw_exec,
                               context : import_expr_context){

        for (exec in cyclix_gen) {
            cyclix_gen.import_expr(debug_lvl, exec, context, ::reconstruct_expression)
        }
    }

    fun translate(debug_lvl : DEBUG_LEVEL) : cyclix.Generic {
        NEWLINE()
        MSG("##############################################")
        MSG("#### Starting Neuromorphix-to-Cyclix translation ####")
        MSG("#### module: " + name)
        MSG("##############################################")

        var cyclix_gen = cyclix.Generic("Neuromorphic_design")
        var TranslateInfo = __TranslateInfo(this)
        var var_dict = mutableMapOf<hw_var, hw_var>()
        var context = import_expr_context(var_dict)

        if (snn.nn_type == NEURAL_NETWORK_TYPE.SFNN) {
            // Generate resources for epochals
//            for (CUR_PROCESS_INDEX in 0 until TranslateInfo.ProcessList.size) {
                // Generating resources for variables
                // processing pContext list
                for (local in locals) {
                    TranslateInfo.__local_assocs.put(local, cyclix_gen.ulocal(local.name, local.defval))
                }

                // processing pContext list
                for (global in globals) {
                    TranslateInfo.__global_assocs.put(global, cyclix_gen.uglobal(global.name, global.defval))
                }

                // Generating Tick for timeslot processing period
                var tick = cyclix_gen.uglobal("tick", "0")
                var tick_period = cyclix_gen.ulocal("tick_period", hw_imm(tick_slot))
                var clk_counter = cyclix_gen.uglobal("clk_counter", "0")
                var next_clk_count = cyclix_gen.uglobal("next_clk_count", "0")
                next_clk_count.assign(clk_counter.plus(1))
                clk_counter.assign(next_clk_count)

                cyclix_gen.begif(cyclix_gen.eq2(tick_period, clk_counter))
                run {
                    tick.assign(1)
                    clk_counter.assign(0)
                }; cyclix_gen.endif()
                cyclix_gen.begelse()
                run {
                    tick.assign(0)
                }; cyclix_gen.endif()

                // Additional logic for generate epochals resources
                for (epochal in epochals) {
                    var new_global = cyclix_gen.global((epochal.name), epochal.vartype, epochal.defimm)
                    var temp_new_global = cyclix_gen.global("gen_epoch" + epochal.name, epochal.vartype, epochal.defimm)
                    cyclix_gen.begif(tick)
                    run {
                        new_global.assign(0)
                    }; cyclix_gen.endif()
                    cyclix_gen.begelse()
                    run {
                        new_global.assign(temp_new_global)
                    }; cyclix_gen.endif()

                    TranslateInfo.__epochal_assocs.put(temp_new_global, new_global)
                    TranslateInfo.__epochal_assocs.put(epochal, temp_new_global)
                }

                var weight_mem_dim = hw_dim_static()
                weight_mem_dim.add(snn.weight_width, 0)
                weight_mem_dim.add(snn.presyn_neur-1, 0)
                weight_mem_dim.add(snn.postsyn_neur-1, 0)
                var weight_memory = cyclix_gen.uglobal("weight_memory", weight_mem_dim, "0" )

                var output_scheduler_dim = hw_dim_static()
                output_scheduler_dim.add(log2(snn.postsyn_neur), 0)
                output_scheduler_dim.add(snn.postsyn_neur, 0)
                var output_scheduler = cyclix_gen.uglobal("output_scheduler", output_scheduler_dim, "0" )

                var input_scheduler_dim = hw_dim_static()
                input_scheduler_dim.add(log2(snn.postsyn_neur), 0)
                input_scheduler_dim.add(snn.presyn_neur * snn.postsyn_neur, 0)
                var input_scheduler = cyclix_gen.uglobal("input_scheduler", input_scheduler_dim, "0" )

                var presyn_neuron_counter_num = cyclix_gen.global("presyn_neuron_counter_num", hw_type(DATA_TYPE.BV_UNSIGNED, "0"), "0")
                var next_presyn_neuron_num = cyclix_gen.global("next_neuron_num", hw_type(DATA_TYPE.BV_UNSIGNED, "0"), "0")
                next_presyn_neuron_num.assign(presyn_neuron_counter_num.plus(1))
                presyn_neuron_counter_num.assign(next_presyn_neuron_num)

                var postsyn_neuron_counter_num = cyclix_gen.global("postsyn_neuron_counter_num", hw_type(DATA_TYPE.BV_UNSIGNED, "0"), "0")
                var next_postsyn_neuron_num = cyclix_gen.global("next_postsyn_neuron_num", hw_type(DATA_TYPE.BV_UNSIGNED, "0"), "0")
                next_postsyn_neuron_num.assign(postsyn_neuron_counter_num.plus(1))
                postsyn_neuron_counter_num.assign(next_postsyn_neuron_num)

                for (buf in i_buffer) {
                    var new_io_buf = hw_fifo(cyclix_gen,"new_io_buf_"+buf.name, log2(snn.postsyn_neur - 1), 0 )
                    var input_spk = cyclix_gen.indexed(input_scheduler, presyn_neuron_counter_num)
                    buf.data_in = input_spk
                    var weight = cyclix_gen.ulocal("weight", "0")
                    weight.assign(weight_memory)
                    var weights_presyn = cyclix_gen.indexed(weight_memory, presyn_neuron_counter_num)
                    var weight_presyn_postsyn = cyclix_gen.indexed(weights_presyn, input_spk)
                    TranslateInfo.__i_buffers.put(buf, new_io_buf)
                }

                for (buf in o_buffer) {
                    var new_io_buf = hw_fifo(cyclix_gen,"new_io_buf_"+buf.name, log2(snn.postsyn_neur - 1), 0 )
                    var ouput_spk = buf.data_out.assign(cyclix_gen.indexed(output_scheduler, postsyn_neuron_counter_num))
                    TranslateInfo.__o_buffers.put(buf, new_io_buf)
                }

                context.var_dict.putAll(TranslateInfo.__local_assocs)
                context.var_dict.putAll(TranslateInfo.__global_assocs)
                context.var_dict.putAll(TranslateInfo.__epochal_assocs)

                MSG("Generating resources: done")

                for (proc in Procs) {
                // TranslateInfo.ProcessList.add(proc.value)
                    println("translate: " + proc.value)
                    for (exec in proc.value.expressions) {
                        cyclix_gen.import_expr(debug_lvl, exec, context, ::reconstruct_expression)
                    }
//                }
            }
        }
        cyclix_gen.end()

        return cyclix_gen
    }

}

fun main(){
//    var sfnn_snn_arch = snn_arch("test", NEURAL_NETWORK_TYPE.SFNN, 32, 32, 4, 5)
//    val mem_models = sfnn_memory_models("sfnn_mem", sfnn_snn_arch)
//
//    println(mem_models.printMemValues())
//
//    var accelerator = Neuromorphic("debug_acc", sfnn_snn_arch, 5000)
//
//    var cyclix_dbg = accelerator.translate(DEBUG_LEVEL.FULL)
//
//    var cpu_rtl = cyclix_dbg.export_to_rtl(DEBUG_LEVEL.FULL)
//
//    var dirname = "test/"
//    cpu_rtl.export_to_sv(dirname + "sverilog", DEBUG_LEVEL.SILENT)

}