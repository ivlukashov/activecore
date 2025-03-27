/*
 *     License: See LICENSE file for details
 */

package leaky

import hwast.*
import neuromorphix.*

val SNN = SnnArch("lif_core")

class LIF(name : String) : Neuromorphic(name, SNN) {
    val synaptic = synaptic_phase("syna", SNN.presyn_neurons, SNN.postsyn_neurons)
    val synaptic_transaction = SynapticTr("synapse")
    val weight = synaptic_transaction.add_field("weight", SNN.weightWidth)

    val soma = somatic_phase("soma", SNN.postsyn_neurons)

    var somatic_transaction = SomaticTr("soma")
    val membrane_potential = somatic_transaction.add_field("membrane_potential", SNN.potentialWidth)

    init {
        synaptic.plus(membrane_potential, weight, SNN.nnType)
        soma.srl(membrane_potential, SNN.leakage)

        soma.begin()
        run{
            soma.begif(membrane_potential.geq(membrane_potential, SNN.threshold))
            run {
                soma.single_spike()
                membrane_potential.assign(SNN.reset)
            };
        }

    }
}