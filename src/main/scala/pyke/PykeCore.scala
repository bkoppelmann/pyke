package pyke

import chisel3._
import chisel3.util._
import soc.ScratchPadPort

import Constants._

class CoreIO extends Bundle {
  val imem = Flipped(new ScratchPadPort(32))
  val dmem = Flipped(new ScratchPadPort(32))
  val fetch_en = Input(Bool())
}

class PykeCore extends Module {
  val io: CoreIO = IO(new CoreIO())

  val rf    = Module(new RegisterFile(4, 2)) // 2 readPorts per Lane, 1 writePort per Lane times 2 Lanes
  val lane0 = Module(new Lane(true, false))
  val lane1 = Module(new Lane(false, true))

  val pc = RegInit(0x80000000L.U(32.W))
  val pc_plus4 = pc + 4.U

  // branch logic
  pc := lane0.io.pc_next

 /*
  * fetch
  */
  io.imem.req.addr    := pc(31, 2)
  io.imem.req.valid   := true.B
  io.imem.req.wr      := false.B
  io.imem.req.wr_mask := DontCare
  io.imem.req.data    := DontCare

  /*
   * Lanes
   */
  val insn = Mux(io.imem.resp.rdy, io.imem.resp.data, NOP)
  lane0.io.insn := Mux(!io.fetch_en, NOP, insn(15,0))  // lane 0
  lane0.io.pc   := pc
  lane0.io.pc_plus4 := Mux(!io.fetch_en, pc, pc_plus4)
  lane0.io.dmem := DontCare
  lane0.io.rfReadPorts(0) <> rf.io.read_ports(0)
  lane0.io.rfReadPorts(1) <> rf.io.read_ports(1)
  lane0.io.rfWritePort    <> rf.io.write_ports(0)

  lane1.io.insn := Mux(!io.fetch_en, NOP, insn(31,16)) // lane 1
  lane1.io.pc   := pc
  lane1.io.pc_plus4 := pc_plus4
  lane1.io.dmem <> io.dmem
  lane1.io.rfReadPorts(0) <> rf.io.read_ports(2)
  lane1.io.rfReadPorts(1) <> rf.io.read_ports(3)
  lane1.io.rfWritePort    <> rf.io.write_ports(1)

}
