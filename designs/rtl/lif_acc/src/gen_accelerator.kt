import hwast.DEBUG_LEVEL
import leaky.*
import neuromorphix.*

fun main(args: Array<String>) {
    println("Generation LIF accelerator")

    var lif_nn_model_256_256_4 = lif_snn("lif_nn_model_256_256_4", 64, 64, 4, 5)
    var lif_accelerator = lif("lif_accelerator", lif_nn_model_256_256_4)

    var cyclix_ast = lif_accelerator.translate(DEBUG_LEVEL.FULL)
    var lif_rtl = cyclix_ast.export_to_rtl(DEBUG_LEVEL.FULL)
    var dirname = "LIF_acceleralor_256_256_4/"
    lif_rtl.export_to_sv(dirname + "sverilog", DEBUG_LEVEL.FULL)

}