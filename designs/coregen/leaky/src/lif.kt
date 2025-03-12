/*
 *     License: See LICENSE file for details
 */

package leaky

import hwast.*
import neuromorphix.*


//// Derived class LifSnn inheriting from SnnArch
// class LifSnn(
//    name: String,
//    nnType: NEURAL_NETWORK_TYPE,
//    presynNeur: Int,
//    postsynNeur: Int,
//    weightWidth: Int,
//    potentialWidth: Int
// ) : SnnArch(name, nnType, presynNeur, postsynNeur, weightWidth, potentialWidth, 2, 15,1,  SPIKE_TYPE.BINARY) {
//
////     Secondary constructor with default nnType
//     constructor(name: String, presynNeur: Int, postsynNeur: Int, weightWidth: Int, potentialWidth: Int)
//            : this(name, NEURAL_NETWORK_TYPE.SFNN, presynNeur, postsynNeur, weightWidth, potentialWidth)
// }
val OP_123 = hwast.hw_opcode("123")


class lif(name : String) : Neuromorphic(name) {
//    val soma = somatic_phase()
//    var somatic_transaction = SomaticTr("soma") //, soma)
//
//    init {
//        val membrane_potential = somatic_transaction.add_field("membrane_potential", 8)
//        val upd_membrane_potential = somatic_transaction.add_field("upd_membrane_potential", 8)
//
////        soma.begin()
////        run {
//            soma.accumulate(upd_membrane_potential, membrane_potential)
////        }
//    }
}

// class lif(name : String, snn : LifSnn, tick_timeslot: Int) : Neuromorphic( name, snn, tick_timeslot) {
//
//    constructor(name : String, snn : LifSnn) : this(name, snn, 15)
//    init {
//        val simple_synapse = createSynapse("simple_synapse")
//        val weight_field = SynapseField("weight", 7, 0)
//        simple_synapse.add_field(weight_field)
//
//        val presyn_neurons = Neurons("presyn", 10)
//        val membrane_potential = presyn_neurons.add_param("membrane_potential", 4)
//        neurons_tr(presyn_neurons)
//        val postsyn_neurons = Neurons("postsyn", 10)
//        postsyn_neurons.add_param("membrane_potential", 4)
//        neurons_tr(postsyn_neurons)
//
//        postsyn_neurons.syn_acc(presyn_neurons, membrane_potential, weight_field, NEURAL_NETWORK_TYPE.SFNN)
//        postsyn_neurons.neuron_right_shift(membrane_potential, 1)
//        postsyn_neurons.neuron_compare(membrane_potential, 1)
//    }
// }