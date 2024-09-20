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

enum class NEURAL_NETWORK_TYPE {
    SFNN, SCNN
    // add memristor based
}

val OP_NEURO = hwast.hw_opcode("neuromorph")
val OP_SYNAPTIC = hwast.hw_opcode("synaptic")


data class SCHEDULER_BUF_SIZE(var SIZE : Int)

data class STATIC_MEMORY_SIZE(val width : Int, val depth : Int)  // N*M matrix of weight[width:0]

data class DYNAMIC_MEMORY_SIZE(val width : Int, val depth : Int)

enum class SPIKE_TYPE {
    BINARY
    // add memristor based
}



open class snn_arch(
    val name : String, val nn_type : NEURAL_NETWORK_TYPE, var presyn_neur : Int, val postsyn_neur : Int,
    val weight_width : Int, val potential_width : Int, val leakage : Int, val threshold : Int, val spikes_type : SPIKE_TYPE ) {

//    val presyn_neurons_num = presyn_neur
//    val postsyn_neurons_num = postsyn_neur

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

class hw_synaptic_handler(val name : String, val neuromorphic: Neuromorphic) : hwast.hw_exec(OP_SYNAPTIC) {

    fun begin() {
        neuromorphic.begsynaptic_operation(this)
    }
}

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

class neuro_epochal(name : String, vartype : hw_type, defimm : hw_imm)
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

class input_buffer(name_prefix : String, val buf_size : Int) {
    var name = name_prefix + "_input_buf"
    var size = buf_size
    var buf_dim = hw_dim_static()
    var mem = neuro_global(name_prefix + "_input_buf_mem", hw_type(DATA_TYPE.BV_UNSIGNED, buf_dim), "0")
}

class output_buffer(name_prefix : String, val buf_size : Int) {
    var name = name_prefix + "_output_buf"
    var size = buf_size
    var buf_dim = hw_dim_static()
    var mem = neuro_global(name_prefix + "_output_buf_mem", hw_type(DATA_TYPE.BV_UNSIGNED, buf_dim), "0")
}

class stat_mem(name_prefix : String, val dim1: Int, val dim2 : Int) {
    var name = name_prefix + "_stat_mem"
//    var size = buf_size
    var mem_dim = hw_dim_static()
    var mem = neuro_global(name_prefix + "_stat_mem", hw_type(DATA_TYPE.BV_UNSIGNED, mem_dim), "0")
}

class dyn_mem(name_prefix : String, val size: Int) {
    var name = name_prefix + "_dyn_mem"
    //    var size = buf_size
    var mem_dim = hw_dim_static()
    var mem = neuro_global(name_prefix + "_dyn_mem", hw_type(DATA_TYPE.BV_UNSIGNED, mem_dim), "0")
}

//class presyn_counter(name : String)
//    : hw_var(name,  hw_type(DATA_TYPE.BV_UNSIGNED, "0"), "0") {
//
//}
//
//class postsyn_counter(name : String)
//    : hw_var(name,  hw_type(DATA_TYPE.BV_UNSIGNED, "0"), "0") {
//
//}

open class dynamic_params( name_prefix: String, transaction : hw_var ){
    var name = name_prefix
    var transaction = transaction
}

internal class __TranslateInfo(var neuromorphic : Neuromorphic) {
    var __global_assocs = mutableMapOf<hw_var, hw_var>()
    var __local_assocs = mutableMapOf<hw_var, hw_var>()
    var __epochal_assocs = mutableMapOf<hw_var, hw_var>()
//    var __processing_handlers_assocs = mutableMapOf<hw_neuro_handler, __neurons_proc_info>()  // neuron_handler
    var ProcessList = ArrayList<hw_neuro_handler>()
//    var ProcessInfoList = ArrayList<__neurons_proc_info>()

//    var __i_buffers = mutableMapOf<i_buffer_if, hw_fifo>()
//    var __o_buffers = mutableMapOf<o_buffer_if, hw_fifo>()

//    var __o_buffers = mutableMapOf<hw_fifo_out, hw_fifo_out>()
//    var __i_buffers = mutableMapOf<hw_fifo_in, hw_fifo_in>()
//
//    var gen_neurons_count = DUMMY_VAR
//    var __genvars_assocs = mutableMapOf<hw_var, hw_var>()
//    var __ports_assocs = mutableMapOf<hw_var, hw_var>()
}

open class Neuromorphic(val name : String, val snn : snn_arch, val tick_slot : Int ) : hw_astc_stdif() {

    var locals = ArrayList<neuro_local>()
    var globals = ArrayList<neuro_global>()
    var epochals = ArrayList<neuro_epochal>()

//    var input_buffers = ArrayList<input_buffer>()
//    var output_buffers = ArrayList<output_buffer>()
//    var stat_mems = ArrayList<stat_mem>()
//    var dyn_mems = ArrayList<dyn_mem>()

//    var PortConnections = mutableMapOf<hw_port, hw_param>()
    var Procs = mutableMapOf<String, hw_neuro_handler>()

//    var presyn_neurons_counter = hw_var("presyn_neurons_counter",hw_type(DATA_TYPE.BV_UNSIGNED, "0"), "0");
//    var postsyn_neurons_counter = hw_var("postsyn_neurons_counter", hw_type(DATA_TYPE.BV_UNSIGNED, "0"), "0");


    fun neuron_handler(name: String): hw_neuro_handler {
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

//    private fun add_port(new_port: hw_port){
//        if (wrvars.containsKey(new_port.name)) ERROR("Naming conflict for port: " + new_port.name)
//        if (rdvars.containsKey(new_port.name)) ERROR("Naming conflict for port: " + new_port.name)
//
//        Ports.add(new_port)
////        rdvars.put(new_port.name, new_port)
////        neur_ports.add(new_port)
//        new_port.default_astc = this
//    }


    fun local(name: String, vartype: hw_type, defimm: hw_imm): neuro_local {
        var ret_var = neuro_local(name, vartype, defimm)
        add_local(ret_var)
        return ret_var
    }

    fun local(name: String, vartype: hw_type, defval: String): neuro_local {
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

    fun epochal(name: String, vartype: hw_type, defval: String): neuro_epochal {
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

//    fun presyn_counter(name: String, msb: Int, lsb: Int, defimm: hw_imm): neuro_epochal {
//        var ret_var = neuro_epochal(name, hw_type(DATA_TYPE.BV_UNSIGNED, msb, lsb), defimm)
//        add_epochal(ret_var)
//        return ret_var
//    }
//
//    fun postsyn_counter(name: String, msb: Int, lsb: Int, defimm: hw_imm): neuro_epochal {
//        var ret_var = neuro_epochal(name, hw_type(DATA_TYPE.BV_UNSIGNED, msb, lsb), defimm)
//        add_epochal(ret_var)
//        return ret_var
//    }

//    fun unsigned_port(name: String, port_dir: PORT_DIR, defval: String): hw_port {
//        var ret_var = hw_port(name, port_dir, hw_type(DATA_TYPE.BV_UNSIGNED, defval), defval)
//        add_port(ret_var)
//        return ret_var
//    }

//    private fun add_input_buffer(new_buffer : input_buffer){
//        input_buffers.add(new_buffer)
//        new_buffer.mem.default_astc = this
//    }
//
//    fun i_if(name_prefix: String, size : Int): input_buffer {
//        var ret_buf = input_buffer(name_prefix, size)
//        add_input_buffer(ret_buf)
//        return ret_buf
//    }
//
//    private fun add_output_buffer(new_buffer : output_buffer){
//        output_buffers.add(new_buffer)
//        new_buffer.mem.default_astc = this
//    }
//
//    fun o_if(name_prefix: String, size : Int): output_buffer {
//        var ret_buf = output_buffer(name_prefix, size)
//        add_output_buffer(ret_buf)
//        return ret_buf
//    }
//
//    private fun add_stat_mem(new_mem : stat_mem){
//        stat_mems.add(new_mem)
//        new_mem.mem.default_astc = this
//    }
//
//    fun mkstaticmem (name_prefix : String, dim1 : Int, dim2 : Int) : stat_mem {
//        var ret_mem = stat_mem(name_prefix, dim1, dim2)
//        add_stat_mem(ret_mem)
//        return ret_mem
//    }
//
//    private fun add_dyn_mem(new_mem : dyn_mem){
//        dyn_mems.add(new_mem)
//        new_mem.mem.default_astc = this
//    }
//
//    fun mkdynamicmem (name_prefix : String, size : Int) : dyn_mem {
//        var ret_mem = dyn_mem(name_prefix, size)
//        add_dyn_mem(ret_mem)
//        return ret_mem
//    }

    fun begproc(neurohandler: hw_neuro_handler) {
        this.add(neurohandler)
    }

    fun begsynaptic_operation(neurohandler: hw_synaptic_handler) {
        this.add(neurohandler)
    }

    fun end_synaptic_operation() {
        this.clear()
    }

    fun endtimeslot() {
        this.clear()
    }

    fun reconstruct_expression(
        debug_lvl: DEBUG_LEVEL,
        cyclix_gen: hw_astc,
        expr: hw_exec,
        context: import_expr_context
    ) {
        cyclix_gen.import_expr(debug_lvl, expr, context, ::reconstruct_expression)
    }

    fun printObjectMethods(obj: Any) {
        val kClass = obj::class

        // Получаем все методы объекта
        val methods = kClass.declaredFunctions + kClass.memberFunctions

        // Печатаем имена методов
        methods.forEach { method ->
            println(method.name)
        }
    }

    fun synaptic_handler(topology:NEURAL_NETWORK_TYPE, snn_model:snn_arch) {

    }

    fun translate(debug_lvl: DEBUG_LEVEL): cyclix.Generic {
        NEWLINE()
        MSG("##############################################")
        MSG("#### Starting Neuromorphix-to-Cyclix translation ####")
        MSG("#### module: " + name)
        MSG("##############################################")

        var cyclix_gen = cyclix.Generic("Neuromorphic_design")
        var var_dict = mutableMapOf<hw_var, hw_var>()
        var context = import_expr_context(var_dict)
        var TranslateInfo = __TranslateInfo(this)


        if (snn.nn_type == NEURAL_NETWORK_TYPE.SFNN) {
            val presyn_neurons = snn.getPresynNeurNum()
            val postsyn_neurons = snn.getPostsynNeurNum()
            val weight_width = snn.getWeightWidth()
            val potential_width = snn.getPotentialWidth()
            val threshold = snn.threshold
            val leak = snn.leakage
            MSG(""+presyn_neurons+postsyn_neurons+weight_width+potential_width)


            // Объявление входного и выходного FIFO
            var input_fifo_dim = hw_dim_static(1)
            input_fifo_dim.add(presyn_neurons*postsyn_neurons-1, 0)
            val fifo_in_interface = cyclix_gen.ufifo_in("fifo_input", input_fifo_dim)

            var output_fifo_dim = hw_dim_static(1)
            output_fifo_dim.add(postsyn_neurons, 0)
            val fifo_out_interface = cyclix_gen.ufifo_out("fifo_output", output_fifo_dim)

            // Память весов
            var weights_mem_dim = hw_dim_static(weight_width)
            weights_mem_dim.add(presyn_neurons-1, 0)  // разрядность веса
            weights_mem_dim.add(postsyn_neurons-1, 0)  // 256
            var weight_memory = cyclix_gen.uglobal("weights_mem", weights_mem_dim, "0")

            // Память статических параметров
            var membrane_potential_mem_dim = hw_dim_static(potential_width)
            membrane_potential_mem_dim.add(postsyn_neurons-1, 0)   // 256
            var membrane_potential_memory = cyclix_gen.uglobal("membrane_potential_memory", membrane_potential_mem_dim, "0")


            // проверка записи/чтения из fifo
            // сигнал для начала чтения
            var sig_rd_fifo = cyclix_gen.uglobal("sig_rd_fifo", hw_dim_static(1), "0")
            var dbg_sig_rd_fifo = cyclix_gen.uport("dbg_sig_rd_fifo", PORT_DIR.IN,  hw_dim_static(1), "0")
            sig_rd_fifo.assign(dbg_sig_rd_fifo)

            var in_fifo_dim = hw_dim_static(1)
            in_fifo_dim.add(presyn_neurons*postsyn_neurons-1, 0)
            var in_fifo = cyclix_gen.uglobal("in_fifo", in_fifo_dim, "0")
            cyclix_gen.begif(cyclix_gen.eq2(sig_rd_fifo, 1))
            run {
                cyclix_gen.fifo_rd_unblk(fifo_in_interface, in_fifo)
            }; cyclix_gen.endif()


            var out_fifo_dim = hw_dim_static(1)
            out_fifo_dim.add(postsyn_neurons-1, 0)
            var out_fifo = cyclix_gen.uglobal("out_fifo", out_fifo_dim, "0")
            cyclix_gen.begif(cyclix_gen.eq2(sig_rd_fifo, 1))
            run {
                cyclix_gen.fifo_rd_unblk(fifo_in_interface, out_fifo)
            }; cyclix_gen.endif()

            // Контроллер для управления счётчиками
            // Внешний сигнал
            val start_processing = cyclix_gen.uport("start_processing", PORT_DIR.IN, hw_dim_static(1), "0")
            val reg_start_processing = cyclix_gen.uglobal("reg_start_processing", hw_dim_static(1), "0")
            reg_start_processing.assign(start_processing)

            // Состояния контроллера
            val STATE_IDLE = 0
            val STATE_PRESYN_PROC = 1
            val STATE_LEAK_FIRE = 2
            val STATE_POSTSYN_PROC = 3
            val STATE_DONE = 4  // Новое состояние для завершения работы

            // Переменная для отслеживания состояния обработки
            val current_state = cyclix_gen.uglobal("current_state", hw_dim_static(3, 0), "0")
            val next_state = cyclix_gen.uglobal("next_state", hw_dim_static(2, 0), "0")

            val postsynapse_neuron_number = cyclix_gen.uglobal("postsynapse_neuron_number", hw_dim_static(3), "0")
            val presynapse_neuron_number = cyclix_gen.uglobal("presynapse_neuron_number", hw_dim_static(3), "0")

            current_state.assign(next_state)

            var input_spike_num = cyclix_gen.uglobal("input_spike_num", hw_dim_static(16), "0")
            val actual_spike = cyclix_gen.uglobal("actual_spike", hw_dim_static(1, 0), "0")
            actual_spike.assign(in_fifo[input_spike_num])

            // Логика контроллера
            cyclix_gen.begif(cyclix_gen.eq2(reg_start_processing, 1))  // Если старт обработки активен
            run {
                cyclix_gen.begif(cyclix_gen.eq2(current_state, STATE_IDLE))  // Состояние "идл"
                run {
                    postsynapse_neuron_number.assign(0)
                    next_state.assign(STATE_PRESYN_PROC)
                }; cyclix_gen.endif()
            }; cyclix_gen.endif()

            // Обработка пресинаптического счётчика
            cyclix_gen.begif(cyclix_gen.eq2(current_state, STATE_PRESYN_PROC))
            run {

                cyclix_gen.begif(cyclix_gen.less(presynapse_neuron_number, presyn_neurons))  // Проверка количества пресинаптических нейронов
                run {
                    input_spike_num.assign(input_spike_num.plus(1))

                    presynapse_neuron_number.assign(presynapse_neuron_number.plus(1))
                    cyclix_gen.begif(cyclix_gen.eq2(actual_spike, 1))
                    run {
                        var weight_presyn_idx = cyclix_gen.ulocal("weight_presyn_idx", hw_dim_static(3, 0), "0")
                        weight_presyn_idx.assign(presynapse_neuron_number-1)
                        var weight_postsyn_idx = cyclix_gen.ulocal("weight_postsyn_idx", hw_dim_static(3, 0), "0")
                        weight_postsyn_idx.assign(postsynapse_neuron_number)
                        var weight_upd = cyclix_gen.ulocal("weight_upd", hw_dim_static(weight_width, 0), "0")
                        weight_upd.assign(weight_memory[postsynapse_neuron_number][presynapse_neuron_number-1])

                        membrane_potential_memory[postsynapse_neuron_number].assign(membrane_potential_memory[postsynapse_neuron_number].plus(weight_memory[postsynapse_neuron_number][presynapse_neuron_number-1]))  // integrate
                    }; cyclix_gen.endif()
                }; cyclix_gen.endif()
                cyclix_gen.begif(cyclix_gen.eq2(presynapse_neuron_number, presyn_neurons))  // Переход после завершения обработки пресинаптических нейронов
                    run {
                        next_state.assign(STATE_LEAK_FIRE)
//                        next_state.assign(STATE_POSTSYN_PROC)
                    }; cyclix_gen.endif()
            }; cyclix_gen.endif()

            // Обработка утечки и сравнения с порогом
            cyclix_gen.begif(cyclix_gen.eq2(current_state, STATE_LEAK_FIRE))
            run {
//                input_spike_num.assign(input_spike_num.minus(1))

                membrane_potential_memory[postsynapse_neuron_number].assign(cyclix_gen.srl(membrane_potential_memory[postsynapse_neuron_number], 1))  // leak
                cyclix_gen.begif(cyclix_gen.less(membrane_potential_memory[postsynapse_neuron_number], threshold))  // threshold-fire
                run {
                    out_fifo[postsynapse_neuron_number].assign(0)
                }; cyclix_gen.endif()
                cyclix_gen.begelse()
                run {
                    out_fifo[postsynapse_neuron_number].assign(1)
                }; cyclix_gen.endif()

                next_state.assign(STATE_POSTSYN_PROC)
            }; cyclix_gen.endif()

            // Обработка постсинаптического счётчика
            cyclix_gen.begif(cyclix_gen.eq2(current_state, STATE_POSTSYN_PROC))
            run {
                cyclix_gen.begif(cyclix_gen.less(postsynapse_neuron_number, postsyn_neurons-1))  // Проверка количества постсинаптических нейронов
                run {
                    postsynapse_neuron_number.assign(postsynapse_neuron_number.plus(1))
                    presynapse_neuron_number.assign(0)
                    next_state.assign(STATE_PRESYN_PROC)  // Возврат к обработке пресинаптического счётчика для следующего постсинаптического нейрона
                }; cyclix_gen.endif()

//                next_state.assign(STATE_DONE)
            }; cyclix_gen.endif()

            // Состояние завершения
            cyclix_gen.begif(cyclix_gen.eq2(current_state, STATE_DONE))
            run {
                next_state.assign(STATE_IDLE)
            }; cyclix_gen.endif()

            // Контроллер

            for (global in globals) {
                TranslateInfo.__global_assocs.put(global, cyclix_gen.uglobal(global.name, global.defval))
            }

        }

        context.var_dict.putAll(TranslateInfo.__global_assocs)
        context.var_dict.putAll(TranslateInfo.__local_assocs)
        context.var_dict.putAll(TranslateInfo.__epochal_assocs)

//        cyclix_gen.end()

        var Dbg = HwDebugWriter("debug_log.txt")
        Dbg.WriteExec(cyclix_gen.proc)
        Dbg.Close()

        for (proc in Procs) {
            for (exec in proc.value.expressions) {
                cyclix_gen.import_expr(debug_lvl, exec, context, ::reconstruct_expression)
            }
        }

        return cyclix_gen
    }

}

fun main(){

}