/*
 *     License: See LICENSE file for details
 */

package leaky

import hwast.*
import neuromorphix.*


class lif_snn(name: String, nn_type : NEURAL_NETWORK_TYPE,  presyn_neur : Int,  postsyn_neur : Int,  weight_width : Int, potential_width : Int)
    : snn_arch(name, nn_type, presyn_neur, postsyn_neur, weight_width, potential_width) {

    constructor(name : String, presyn_neur : Int,  postsyn_neur : Int,  weight_width : Int, potential_width: Int)
            : this(name, NEURAL_NETWORK_TYPE.SFNN, presyn_neur, postsyn_neur, weight_width, potential_width)
}

class lif(name : String, snn : lif_snn, tick_timeslot: Int) : Neuromorphic( name, snn, tick_timeslot) {

    constructor(name : String, snn : lif_snn) : this(name, snn, 5000)

    var input_if =  input_buffer("input_spikes_buffer")  // input buffer
    var input_spike = ulocal("input_spike", "0")
    var threshold = uglobal("threshold", hw_imm("5"))
    var leakage = uglobal("leakage", hw_imm("1"))
    var membr_potential_mem_dim = hw_dim_static()
    var membrane_potential = uepochal("membrane_potential", membr_potential_mem_dim,"0" )
    var updated_membr_potential  = uepochal("res_potential", "0" )
    var res_membr_potential  = uepochal("res_potential", "0" )
    var output_spike = ulocal("output_spike", "0")
    var output_if = output_buffer("output_spikes_buffer", output_spike)

    var LIF = neuron_handler("LIF")

    init {
        LIF.begin()  // timeslot operation
        run{
            input_spike.assign(input_if.mem[presyn_nuerons_counter])

            membr_potential_mem_dim.add(3, 0)
            membr_potential_mem_dim.add(snn.postsyn_neur, 0)
            updated_membr_potential.assign(membrane_potential[0][postsyn_neurons_counter] + spike_transaction)

            begif(eq2(presyn_neurons_counter, snn.presyn_neur))
            run {
                res_membr_potential.assign(updated_membr_potential - leakage)
                begif(less(res_membr_potential, threshold))
                run{
                    output_if.tr_out[postsyn_neurons_counter].assign(0)
                }; endif()
                begelse()
                run {
                    output_if.tr_out[postsyn_neurons_counter].assign(1)
                }; endif()
            }; endif()

        }; endtimeslot()
    }

}