package pyke

import chisel3._
import chisel3.util._
import soc.ScratchPadPort

import Constants._

class CoreIO extends Bundle {
  val imem = Flipped(new ScratchPadPort(32))
  val dmem = Flipped(new ScratchPadPort(32))
}

class PykeCore extends Module {
  val io: CoreIO = IO(new CoreIO())

  val lane0 = Module(new Lane)
  val lane1 = Module(new Lane)

  val pc = RegInit(0x80000000L.U(32.W))
  val pc_next = pc + 4.U

 /*
  * fetch
  */
  io.imem.req.addr    := pc(31, 2)
  io.imem.req.valid   := true.B
  io.imem.req.wr      := false.B
  io.imem.req.wr_mask := DontCare
  io.imem.req.data    := DontCare

  /*
   * decode
   */
  val insn = Mux(io.imem.resp.rdy, io.imem.resp.data, NOP)
  lane0.io.insn := insn(15,0)  // lane 0
  lane0.io.pc   := pc
  lane0.io.pc_next := pc_next
  lane0.io.dmem <> io.dmem

  lane1.io.insn := insn(31,16) // lane 1
  lane1.io.pc   := pc
  lane1.io.pc_next := pc_next
  lane1.io.dmem <> io.dmem
  //io.dmem := DontCare
}
