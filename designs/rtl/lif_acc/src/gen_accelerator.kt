import hwast.DEBUG_LEVEL
import leaky.*
import neuromorphix.*
import hwast.*

fun main(args: Array<String>) {
    println("Generation LIF accelerator")

    var lif_core = LIF("n_core")
    var cyclix_ast = lif_core.translate(DEBUG_LEVEL.FULL)

    var dirname = "n_core/"

    var Dbg = HwDebugWriter(dirname + "debug_log.txt")
    Dbg.WriteExec(cyclix_ast.proc)
    Dbg.Close()

    var lif_rtl = cyclix_ast.export_to_rtl(DEBUG_LEVEL.FULL)
    lif_rtl.export_to_sv(dirname + "sverilog", DEBUG_LEVEL.FULL)
}