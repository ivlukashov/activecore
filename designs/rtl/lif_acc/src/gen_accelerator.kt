import hwast.DEBUG_LEVEL
//import leaky.*
import neuromorphix.*
import hwast.*

fun main(args: Array<String>) {
    println("Generation LIF accelerator")

    val nn_model = SnnArch()


    var lif_core =  Neuromorphic("n_core", nn_model)  //  LIF("n_core")
//    var cyclix_ast = lif_core.translate(DEBUG_LEVEL.FULL)
//    var cyclix_ast = lif_core.core_generate(DEBUG_LEVEL.FULL, nn_model)
    var cyclix_ast = lif_core.input_queue_gen(nn_model)
    var dirname = "n_core/"

    var Dbg = HwDebugWriter(dirname + "debug_log.txt")
    Dbg.WriteExec(cyclix_ast.proc)
    Dbg.Close()

    var lif_rtl = cyclix_ast.export_to_rtl(DEBUG_LEVEL.FULL)
    lif_rtl.export_to_sv(dirname + "sverilog", DEBUG_LEVEL.FULL)
}

//fun main(args: Array<String>) {
//    println("Generation LIF accelerator")
//
//    var lif_nn_model = LifSnn("lif_accs", 10, 10, 16, 16)
//    var lif_accelerator = lif("lif_accelerator", lif_nn_model)
//    var cyclix_ast = lif_accelerator.translate(DEBUG_LEVEL.FULL)
//
//    var Dbg = HwDebugWriter("debug_log.txt")
//    Dbg.WriteExec(cyclix_ast.proc)
//    Dbg.Close()
//
//    var lif_rtl = cyclix_ast.export_to_rtl(DEBUG_LEVEL.FULL)
//    var dirname = "LIF_acceleralor_255/"
//    lif_rtl.export_to_sv(dirname + "sverilog", DEBUG_LEVEL.FULL)
//
//}