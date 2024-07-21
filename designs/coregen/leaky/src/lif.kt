/*
 *     License: See LICENSE file for details
 */

package leaky

import hwast.*
import neuromorphix.*


class lif_snn(name: String, nn_type : NEURAL_NETWORK_TYPE,  presyn_neur : Int,  postsyn_neur : Int,  weight_width : Int, potential_width : Int)
    : snn_arch(name, nn_type, presyn_neur, postsyn_neur, weight_width, potential_width, 2, 1, SPIKE_TYPE.BINARY) {

    constructor(name : String, presyn_neur : Int,  postsyn_neur : Int,  weight_width : Int, potential_width: Int)
            : this(name, NEURAL_NETWORK_TYPE.SFNN, presyn_neur, postsyn_neur, weight_width, potential_width)
}

class lif(name : String, snn : lif_snn, tick_timeslot: Int) : Neuromorphic( name, snn, tick_timeslot) {

    constructor(name : String, snn : lif_snn) : this(name, snn, 5000)

//    var input_if =  input_buffer("input_spikes_buffer")  // input buffer
//    var input_spike = ulocal("input_spike", "0")
//    var threshold = uglobal("threshold", hw_imm("5"))
//    var leakage = uglobal("leakage", hw_imm("1"))
//    var membr_potential_mem_dim = hw_dim_static()
//    var membrane_potential = uepochal("membrane_potential", membr_potential_mem_dim,"0" )
//    var updated_membr_potential  = uepochal("res_potential", "0" )
//    var res_membr_potential  = uepochal("res_potential", "0" )
//    var output_spike = ulocal("output_spike", "0")
//    var output_if = output_buffer("output_spikes_buffer", output_spike)

    var input_scheduler =  mkbuffer("input_scheduler", snn.presyn_neur)  // input buffer
    var output_scheduler =  mkbuffer("output_scheduler", snn.postsyn_neur)
    var weights_memory = mkstaticmem("weights", snn.presyn_neur, snn.postsyn_neur)
    var potentials_memory = mkdynamicmem("potentials", snn.postsyn_neur)
    var potential = uepochal("potential", "0")
    var leakage = uglobal("leakage", hw_imm("1"))

    var LIF = neuron_handler("LIF")

    init {
        LIF.begin()  // timeslot operation
        run{
            begif(less(postsyn_neurons_counter, snn.postsyn_neur))
            run {
                begif(less(presyn_neurons_counter, snn.presyn_neur))
                // updating membraine potentials
                run {
                    begif(eq2(input_scheduler.mem[presyn_neurons_counter], 1))
                        run {
                            potentials_memory.mem[postsyn_neurons_counter].assign(weights_memory.mem[postsyn_neurons_counter][presyn_neurons_counter])
                        }; endif()
                    }; endif()

                begelse()
                run {
                    potential.assign(potentials_memory.mem[postsyn_neurons_counter].minus(leakage))  // leakage

                    begif(less(potential, snn.threshold))
                    run {
                        output_scheduler.mem[postsyn_neurons_counter].assign(0)    // not fire
                    }; endif()

                    begelse()
                    run {
                        output_scheduler.mem[postsyn_neurons_counter].assign(1)    // fire
                    }; endif()
                }; endif()
            }; endif()
        }; endtimeslot()
    }
}