package pyke

import chisel3._
import chisel3.util._
import soc.ScratchPadPort

class CoreIO extends Bundle {
  val imem = Flipped(new ScratchPadPort(32))
  val dmem = Flipped(new ScratchPadPort(32))
}

class PykeCore extends Module {
    val io: CoreIO = IO(new CoreIO())
    io.imem := DontCare
    io.dmem := DontCare

}
