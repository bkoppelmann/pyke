package pyke

import chisel3._
import chisel3.util._
import config.YamlConfig
import soc.{ScratchPad,ScratchPadRequest, DebugOffChipIO, DebugModule}

class PykeTop()(implicit config: YamlConfig) extends Module {
    val io = IO(new Bundle {
    // this indicates whether our testbench is successfull
    // 00 -> fail (simulation ended and software did neither write fail nor
    //       success
    // 01 -> fail (software wrote fail)
    // 11 -> success (software wrote success)
    val success = Output(UInt(2.W))
    val debug = new DebugOffChipIO
  })

  val cpu = Module(new PykeCore)
  val imem = Module(new ScratchPad(1000, config.isa.xLen, config.isa.insnLen, 0x8000L, false))
  val dmem = Module(new ScratchPad(1000, config.isa.xLen, config.isa.xLen, 0xa0000L, true))
  val debug = Module(new DebugModule)

  when (io.debug.fetch_en) {
    cpu.io.imem <> imem.io
    debug.io.imem := DontCare
  } .otherwise {
    debug.io.imem <> imem.io
    cpu.io.imem := DontCare
  }

  cpu.io.dmem <> dmem.io
  cpu.io.fetch_en := debug.io.cpu_fetch_en

  debug.io.off <> io.debug

  // the success value is memory mapped to the last dmem entry
  io.success := dmem.io.succ
}

