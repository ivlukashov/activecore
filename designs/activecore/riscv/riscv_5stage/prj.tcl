## See LICENSE file for license details

set MLIP_PATH /home/alexander/work/activecore/prj/mlip
load [file join $MLIP_PATH activecore/cpp/Release/libactivecore.so]
source [file join $MLIP_PATH activecore activecore.tcl]

#ActiveCore::debug_set
ActiveCore_Reset

source riscv.tcl

set filename riscv.v

rtl::monitor debug_rtl.txt
pipe::monitor debug_pipe.txt

ActiveCore::export verilog $filename