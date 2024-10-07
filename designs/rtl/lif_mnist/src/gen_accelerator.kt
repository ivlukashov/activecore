import hwast.DEBUG_LEVEL
import leaky.*
import neuromorphix.*
import hwast.*

fun main(args: Array<String>) {
    println("Generation LIF accelerator")

    var lif_nn_model_256_256_4 = lif_snn("lif_nn_model_256_256_4", 3, 3, 4, 4)
    var lif_accelerator = lif("lif_accelerator", lif_nn_model_256_256_4)

    var cyclix_ast = lif_accelerator.translate(DEBUG_LEVEL.FULL)
    for (i in cyclix_ast){
        println("cyclix_ast:" + i)
    }

    var Dbg = HwDebugWriter("debug_log.txt")
    Dbg.WriteExec(cyclix_ast.proc)
    Dbg.Close()

    var lif_rtl = cyclix_ast.export_to_rtl(DEBUG_LEVEL.FULL)
    var dirname = "LIF_acceleralor_256_256_4/"
    lif_rtl.export_to_sv(dirname + "sverilog", DEBUG_LEVEL.FULL)

}