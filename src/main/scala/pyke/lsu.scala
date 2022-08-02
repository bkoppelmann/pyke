package pyke

import chisel3._
import chisel3.util._
import Constants._

import soc.ScratchPadPort
import config.YamlConfig

class LoadStoreUnitPorts()(implicit config:YamlConfig) extends Bundle {
  val dmem = Flipped(new ScratchPadPort(32, 32))
  val wr = Input(Bool())
  val r_data = Output(UInt(32.W))
  val wr_data = Input(UInt(32.W))
  val addr = Input(UInt(32.W))
  val en = Input(Bool())
}

class LoadStoreUnit()(implicit config:YamlConfig) extends Module {
  val io = IO(new LoadStoreUnitPorts)


  // default here, so we don't need to defines them
  // in all cases
  io.dmem.req.valid    := false.B
  io.dmem.req.wr       := false.B
  io.dmem.req.addr     := DontCare
  io.dmem.req.data     := DontCare
  io.dmem.req.wr_mask  := DontCare
  io.dmem.resp.data    := DontCare

  io.r_data := 0xdead.U(32.W)

  // access memory
  io.dmem.req.addr := io.addr(31, 2)
  io.dmem.req.valid := io.en

  when(io.wr) { // store
    io.dmem.req.wr := true.B
    io.dmem.req.data := io.wr_data
    io.dmem.req.wr_mask := VecInit(true.B, true.B, true.B, true.B)
  } .otherwise { // load
    when (io.dmem.resp.rdy) {
        io.r_data := io.dmem.resp.data
    }
  }

}
