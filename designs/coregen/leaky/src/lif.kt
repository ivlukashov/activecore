/*
 *     License: See LICENSE file for details
 */

package leaky

import hwast.*
import neuromorphix.*
import cyclix.STAGE_FC_MODE
import cyclix.hw_local


class lif_snn(name: String, nn_type : NEURAL_NETWORK_TYPE,  presyn_neur : Int,  postsyn_neur : Int,  weight_width : Int, potential_width : Int)
    : snn_arch(name, nn_type, presyn_neur, postsyn_neur, weight_width, potential_width) {

    constructor(name : String, presyn_neur : Int,  postsyn_neur : Int,  weight_width : Int, potential_width: Int)
            : this(name, NEURAL_NETWORK_TYPE.SFNN, presyn_neur, postsyn_neur, weight_width, potential_width)
}

//open class LIF_accelerator(name : String): Neuromorphic("LIF_accelerator", NEURAL_NETWORK_TYPE.SFNN, 5000) {
class lif(name : String, snn : lif_snn, tick_timeslot: Int) : Neuromorphic( name, snn, tick_timeslot) {

    constructor(name : String, snn : lif_snn) : this(name, snn, 5000)
    var input_spike = hw_local("input_spike", hw_type(DATA_TYPE.BV_UNSIGNED, "0"), "0")
    var input_if = i_buffer("input_spikes_buffer", 1024, 0, input_spike)  // input buffer
    var memrane_potential = uepochal("membrane_potential", 0 )
    var threshold = global("threshold", hw_type(DATA_TYPE.BV_UNSIGNED, "31"), hw_imm("31"))
    var out_spike = hw_local("out_spike", hw_type(DATA_TYPE.BV_UNSIGNED, "0"), "0")
    var output_if = o_buffer("output_spikes_buffer", 1024, 0, out_spike)
    var leakage = ulocal("leakage", "8")

    var LIF = neuron_handler("LIF")

    init {
        LIF.begin()  // здесь операция таймслота
        run{
            // receive_spikes
            input_spike.assign(input_if.data_in)
            // update_memrane_potential
            memrane_potential.assign(input_spike.plus(memrane_potential))
            // leakage
            memrane_potential.assign(memrane_potential.minus(leakage))
            // compare threshold
            out_spike.assign(gr(memrane_potential, threshold))
            begif(out_spike)
            run {
                output_if.data_out.assign(out_spike)
            }; endif()

        }; endtimeslot()
    }



}