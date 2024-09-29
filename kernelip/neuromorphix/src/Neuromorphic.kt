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

enum class NEURAL_NETWORK_TYPE {
    SFNN, SCNN
}

val OP_NEURO = hwast.hw_opcode("neuron_op")
val OP_SYNAPTIC = hwast.hw_opcode("synaptic")



data class SCHEDULER_BUF_SIZE(var SIZE : Int)

data class STATIC_MEMORY_SIZE(val width : Int, val depth : Int)  // N*M matrix of weight[width:0]

data class DYNAMIC_MEMORY_SIZE(val width : Int, val depth : Int)

enum class SPIKE_TYPE {
    BINARY
}

open class SnnArch(
    var name: String = "Default Name",
    var nnType: NEURAL_NETWORK_TYPE = NEURAL_NETWORK_TYPE.SFNN,
    var presynNeur: Int = 10,
    var postsynNeur: Int = 10,
    var outputNeur: Int = 10,
    var weightWidth: Int = 10,
    var potentialWidth: Int = 10,
    var leakage: Int = 1,
    var threshold: Int = 1,
    var spikesType: SPIKE_TYPE = SPIKE_TYPE.BINARY
) {

    fun loadModelFromJson(jsonFilePath: String) {
        val jsonString = File(jsonFilePath).readText()

        val jsonObject = JSONObject(jsonString)

        val modelTopology = jsonObject.getJSONObject("model_topology")
        this.presynNeur = modelTopology.optInt("input_size", this.presynNeur)
        this.postsynNeur = modelTopology.optInt("hidden_size", this.postsynNeur)
        this.outputNeur = modelTopology.optInt("output_size", this.outputNeur)
        val lifNeurons = jsonObject.getJSONObject("LIF_neurons").getJSONObject("lif1")
        this.threshold = lifNeurons.optInt("threshold", this.threshold)
        this.leakage = lifNeurons.optInt("leakage", this.leakage)

        val nnTypeStr = jsonObject.optString("nn_type", "SFNN")
        this.nnType = NEURAL_NETWORK_TYPE.valueOf(nnTypeStr)
    }

    fun getArchitectureInfo(): String {
        return "$name: (NN Type: $nnType, Presynaptic Neurons = $presynNeur, Postsynaptic Neurons = $postsynNeur, Spikes Type: $spikesType)"
    }
}

//data class SYNAPSE_FIELD(val name: String, val msb : Int, val lsb: Int)

class SynapseField(val name: String, val msb : Int, val lsb: Int) {
//    fun field_acc()
}

class Synapse(val name: String) {

    var fields = ArrayList<SynapseField>()

    fun add_field(field_var: SynapseField): SynapseField {
        val new_field = field_var
        fields.add(new_field)

        return new_field
    }
}

class hw_neuron_handler(val name : String, val neuromorphic: Neuromorphic) : hwast.hw_exec(OP_NEURO) {

    fun begin() {
        neuromorphic.begproc(this)
    }
}

class hw_synaptic_handler(val name : String, val neuromorphic: Neuromorphic) : hwast.hw_exec(OP_SYNAPTIC) {

    fun begin() {
        neuromorphic.begsynaptic_operation(this)
    }
}

class tick(name : String , vartype : hw_type, defimm : hw_imm)
    : hw_var(name, vartype, defimm) {

    constructor()
            : this("tick_signal", hw_type(DATA_TYPE.BV_UNSIGNED, 0, 0), hw_imm("0"))
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

open class dynamic_params( name_prefix: String, transaction : hw_var ){
    var name = name_prefix
    var transaction = transaction
}

internal class __TranslateInfo(var neuromorphic : Neuromorphic) {
    var __global_assocs = mutableMapOf<hw_var, hw_var>()
    var __local_assocs = mutableMapOf<hw_var, hw_var>()
    var __epochal_assocs = mutableMapOf<hw_var, hw_var>()
    var ProcessList = ArrayList<hw_neuron_handler>()
    var __tick_assocs = mutableMapOf<hw_var, hw_var>()
}

open class Neuromorphic(val name : String, val snn : SnnArch, val tick_slot : Int ) : hw_astc_stdif() {

    var locals = ArrayList<neuro_local>()
    var globals = ArrayList<neuro_global>()
    var epochals = ArrayList<neuro_epochal>()
    var Neurons_Processes = mutableMapOf<String, hw_neuron_handler>()

    var Synapses = ArrayList<Synapse>()

    var Synaptic_Processes = mutableMapOf<String, hw_synaptic_handler>()

    val presyn_neurons = snn.presynNeur
    val postsyn_neurons = snn.postsynNeur
    val weight_width = snn.weightWidth
    val potential_width = snn.potentialWidth
    val threshold = snn.threshold
    val leak = snn.leakage

    val layers = 2
    val conncted = NEURAL_NETWORK_TYPE.SFNN


    fun neuron_handler(name: String): hw_neuron_handler {
        var new_neuron_process = hw_neuron_handler(name, this)
        Neurons_Processes.put(new_neuron_process.name, new_neuron_process)

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

    internal fun tick_generation(tick_signal: hw_var, timeslot: Int, units: String, clk_period: Int, cyclix_gen: Generic) {  // timeslot in ms, clk_period in ns
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

        var tick_period = cyclix_gen.uglobal("tick_period", hw_imm(timeslot))
        var clk_counter = cyclix_gen.uglobal("clk_counter", "0")
        var next_clk_count = cyclix_gen.uglobal("next_clk_count", "0")

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

    fun createSynapse(synapseName: String): Synapse {
        val newSynapse = Synapse(synapseName)
        Synapses.add(newSynapse)
        return newSynapse
    }

    internal fun static_memory_generation(synapse: Synapse, name: String, neuromorphic: Neuromorphic, cyclix_gen: Generic): hw_var {
        val word_width = synapse.fields.sumOf { it.msb - it.lsb + 1 }
        var static_mem_dim = hw_dim_static(word_width)         // Создание измерения памяти на основе ширины синапса
        static_mem_dim.add(neuromorphic.presyn_neurons - 1, 0)  // разрядность веса
        static_mem_dim.add(neuromorphic.postsyn_neurons - 1, 0)  // 256
        var static_memory = cyclix_gen.sglobal(name, static_mem_dim, "0")  // Создание глобальной памяти

        var current_position = 0    // Инициализация начальной позиции для срезов
        // Цикл по всем полям синапса
        for (field in synapse.fields) {
            val field_width = field.msb - field.lsb + 1    // Рассчитываем размеры среза для текущего поля
            println(field_width)
            val mem_slice = static_memory.GetFracRef(current_position + field.msb, current_position + field.lsb)    // Создаем срез для данного поля

            val mem_slice_dim = hw_dim_static(field_width)
            mem_slice_dim.add(neuromorphic.presyn_neurons - 1, 0)
            mem_slice_dim.add(neuromorphic.postsyn_neurons - 1, 0)

            val slice = cyclix_gen.uglobal("${field.name}_slice", mem_slice_dim, "0")
            slice.assign(mem_slice)   // Присваиваем срезу значение из памяти
            current_position += field_width    // Обновляем текущую позицию для следующего поля
        }

        return static_memory
    }


    fun begproc(neurohandler: hw_neuron_handler) {
        this.add(neurohandler)
    }

    fun begsynaptic_operation(neurohandler: hw_synaptic_handler) {
        this.add(neurohandler)
    }

    fun end_synaptic_operation() {
        this.clear()
    }

    fun endtimeslot() {
//        this.clear()
    }

    fun reconstruct_expression(
        debug_lvl: DEBUG_LEVEL,
        cyclix_gen: hw_astc,
        expr: hw_exec,
        context: import_expr_context
    ) {
        cyclix_gen.import_expr(debug_lvl, expr, context, ::reconstruct_expression)
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


//        // generation tick
//        var tick =  cyclix_gen.uglobal("tick", "0")
//        tick_generation(tick, 50, "ns", 10, cyclix_gen)
//
//        for (synapse in Synapses) {
//            static_memory_generation(synapse, synapse.name + "_stat_mem", this, cyclix_gen)
//        }

        for (neuron_operation in Neurons_Processes) {  }

        for (synaptic_operation in Synaptic_Processes) {

        }

        if (snn.nnType == NEURAL_NETWORK_TYPE.SFNN) {
//            val presyn_neurons = snn.presynNeur
//            val postsyn_neurons = snn.postsynNeur
//            val weight_width = snn.weightWidth
//            val potential_width = snn.potentialWidth
//            val threshold = snn.threshold
//            val leak = snn.leakage
//            MSG(""+presyn_neurons+postsyn_neurons+weight_width+potential_width)
//
//
////            // Объявление входного и выходного FIFO
////            var input_fifo_dim = hw_dim_static(1)
////            input_fifo_dim.add(presyn_neurons-1, 0)
////            val fifo_in_interface = cyclix_gen.ufifo_in("fifo_input", input_fifo_dim)
//
//            var output_fifo_dim = hw_dim_static(1)
//            output_fifo_dim.add(postsyn_neurons, 0)
//            val fifo_out_interface = cyclix_gen.ufifo_out("fifo_output", output_fifo_dim)
//
//            // Память весов
//            var weights_mem_dim = hw_dim_static(weight_width)
//            weights_mem_dim.add(presyn_neurons-1, 0)  // разрядность веса
//            weights_mem_dim.add(postsyn_neurons-1, 0)  // 256
//            var weight_memory = cyclix_gen.sglobal("weights_mem", weights_mem_dim, "0")
//
//            // Память статических параметров
//            var membrane_potential_mem_dim = hw_dim_static(potential_width)
//            membrane_potential_mem_dim.add(postsyn_neurons-1, 0)   // 256
//            var membrane_potential_memory = cyclix_gen.sglobal("membrane_potential_memory", membrane_potential_mem_dim, "0")
//
//
//            // проверка записи/чтения из fifo
//            // сигнал для начала чтения
//            var sig_rd_fifo = cyclix_gen.uglobal("sig_rd_fifo", hw_dim_static(1), "0")
//            var dbg_sig_rd_fifo = cyclix_gen.uport("dbg_sig_rd_fifo", PORT_DIR.IN,  hw_dim_static(1), "0")
//            sig_rd_fifo.assign(dbg_sig_rd_fifo)
//
//            var in_fifo_dim = hw_dim_static(1)
//            in_fifo_dim.add(presyn_neurons*postsyn_neurons-1, 0)
//            var in_fifo = cyclix_gen.uglobal("in_fifo", in_fifo_dim, "0")
////            cyclix_gen.begif(cyclix_gen.eq2(sig_rd_fifo, 1))
////            run {
//////                cyclix_gen.fifo_rd_unblk(fifo_in_interface, in_fifo)
////            }; cyclix_gen.endif()
//
//
//            var out_fifo_dim = hw_dim_static(1)
//            out_fifo_dim.add(postsyn_neurons-1, 0)
//            var out_fifo = cyclix_gen.uglobal("out_fifo", out_fifo_dim, "0")
//            cyclix_gen.begif(cyclix_gen.eq2(sig_rd_fifo, 1))
//            run {
////                cyclix_gen.fifo_rd_unblk(fifo_in_interface, out_fifo)
//            }; cyclix_gen.endif()
//
//            // Контроллер для управления счётчиками
//            // Внешний сигнал
//            val start_processing = cyclix_gen.uport("start_processing", PORT_DIR.IN, hw_dim_static(1), "0")
//            val reg_start_processing = cyclix_gen.uglobal("reg_start_processing", hw_dim_static(1), "0")
//            reg_start_processing.assign(start_processing)
//
//            // Состояния контроллера
//            val STATE_IDLE = 0
//            val STATE_PRESYN_PROC = 1
//            val STATE_LEAK_FIRE = 2
//            val STATE_POSTSYN_PROC = 3
//            val STATE_DONE = 4  // Новое состояние для завершения работы
//
//            // Переменная для отслеживания состояния обработки
//            val current_state = cyclix_gen.uglobal("current_state", hw_dim_static(3, 0), "0")
//            val next_state = cyclix_gen.uglobal("next_state", hw_dim_static(2, 0), "0")
//
//            val postsynapse_neuron_number = cyclix_gen.uglobal("postsynapse_neuron_number", hw_dim_static(10), "0")  // todo: log
//            val presynapse_neuron_number = cyclix_gen.uglobal("presynapse_neuron_number", hw_dim_static(10), "0")
//
//            current_state.assign(next_state)
//
//            var input_spike_num = cyclix_gen.uglobal("input_spike_num", hw_dim_static(16), "0")
//            val actual_spike = cyclix_gen.uglobal("actual_spike", hw_dim_static(1, 0), "0")
//            actual_spike.assign(in_fifo[input_spike_num])
//
//            // Логика контроллера
//            cyclix_gen.begif(cyclix_gen.eq2(reg_start_processing, 1))  // Если старт обработки активен
//            run {
//                cyclix_gen.begif(cyclix_gen.eq2(current_state, STATE_IDLE))  // Состояние "идл"
//                run {
//                    postsynapse_neuron_number.assign(0)
//                    next_state.assign(STATE_PRESYN_PROC)
//                }; cyclix_gen.endif()
//            }; cyclix_gen.endif()
//
//            // Обработка пресинаптического счётчика
//            cyclix_gen.begif(cyclix_gen.eq2(current_state, STATE_PRESYN_PROC))
//            run {
//
//                cyclix_gen.begif(cyclix_gen.less(presynapse_neuron_number, presyn_neurons))  // Проверка количества пресинаптических нейронов
//                run {
//                    input_spike_num.assign(input_spike_num.plus(1))
//
//                    var weight_presyn_idx = cyclix_gen.uglobal("weight_presyn_idx", hw_dim_static(10, 0), "0")
//                    weight_presyn_idx.assign(presynapse_neuron_number-1)
//                    var weight_postsyn_idx = cyclix_gen.uglobal("weight_postsyn_idx", hw_dim_static(10, 0), "0")
//                    weight_postsyn_idx.assign(postsynapse_neuron_number)
//                    var weight_upd = cyclix_gen.slocal("weight_upd", hw_dim_static(weight_width, 0), "0")
//                    weight_upd.assign(weight_memory[postsynapse_neuron_number][presynapse_neuron_number-1])
//
//                    presynapse_neuron_number.assign(presynapse_neuron_number.plus(1))
//                    cyclix_gen.begif(cyclix_gen.eq2(actual_spike, 1))
//                    run {
//                        membrane_potential_memory[postsynapse_neuron_number].assign(membrane_potential_memory[postsynapse_neuron_number].plus(weight_memory[postsynapse_neuron_number][presynapse_neuron_number-1]))  // integrate
//                    }; cyclix_gen.endif()
//                }; cyclix_gen.endif()
//                cyclix_gen.begif(cyclix_gen.eq2(presynapse_neuron_number, presyn_neurons))  // Переход после завершения обработки пресинаптических нейронов
//                    run {
//                        next_state.assign(STATE_LEAK_FIRE)
////                        next_state.assign(STATE_POSTSYN_PROC)
//                    }; cyclix_gen.endif()
//            }; cyclix_gen.endif()
//
//            // Обработка утечки и сравнения с порогом
//            cyclix_gen.begif(cyclix_gen.eq2(current_state, STATE_LEAK_FIRE))
//            run {
////                input_spike_num.assign(input_spike_num.minus(1))
//
//                membrane_potential_memory[postsynapse_neuron_number].assign(cyclix_gen.sra(membrane_potential_memory[postsynapse_neuron_number], leak))  // leak
//                cyclix_gen.begif(cyclix_gen.less(membrane_potential_memory[postsynapse_neuron_number], threshold))  // threshold-fire
//                run {
//                    out_fifo[postsynapse_neuron_number].assign(0)
//                }; cyclix_gen.endif()
//                cyclix_gen.begelse()
//                run {
//                    out_fifo[postsynapse_neuron_number].assign(1)
//                }; cyclix_gen.endif()
//
//                next_state.assign(STATE_POSTSYN_PROC)
//            }; cyclix_gen.endif()
//
//            // Обработка постсинаптического счётчика
//            cyclix_gen.begif(cyclix_gen.eq2(current_state, STATE_POSTSYN_PROC))
//            run {
//                cyclix_gen.begif(cyclix_gen.less(postsynapse_neuron_number, postsyn_neurons-1))  // Проверка количества постсинаптических нейронов
//                run {
//                    postsynapse_neuron_number.assign(postsynapse_neuron_number.plus(1))
//                    presynapse_neuron_number.assign(0)
//                    next_state.assign(STATE_PRESYN_PROC)  // Возврат к обработке пресинаптического счётчика для следующего постсинаптического нейрона
//                }; cyclix_gen.endif()
//
////                next_state.assign(STATE_DONE)
//            }; cyclix_gen.endif()
//
//            // Состояние завершения
//            cyclix_gen.begif(cyclix_gen.eq2(current_state, STATE_DONE))
//            run {
//                next_state.assign(STATE_IDLE)
//            }; cyclix_gen.endif()
//
//            // Контроллер
//
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

        for (proc in Neurons_Processes) {
            for (exec in proc.value.expressions) {
                cyclix_gen.import_expr(debug_lvl, exec, context, ::reconstruct_expression)
            }
        }

        return cyclix_gen
    }

}

fun main(){

}