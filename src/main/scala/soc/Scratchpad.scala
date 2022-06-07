package soc

import chisel3._
import chisel3.util.Cat

class ScratchPadRequest extends Bundle {
  val addr: UInt = Input(UInt(32.W))
  val data: UInt = Input(UInt(32.W))
  val wr: Bool = Input(Bool())
  val wr_mask = Vec(4, Input(Bool()))
  val valid: Bool = Input(Bool())
}

class ScratchPadResponse(dWidth:Int) extends Bundle {
  val data: UInt = Output(UInt(dWidth.W))
  val rdy: Bool = Output(Bool())
}

class ScratchPadPort(dWidth:Int) extends Bundle {
  val req = new ScratchPadRequest
  val resp = new ScratchPadResponse(dWidth)
  val succ: UInt = Output(UInt(2.W))
}

class ScratchPad(size:Int, dWidth:Int, addr_offset:BigInt, wr_mask:Boolean) extends Module {
  val io: ScratchPadPort = IO(new ScratchPadPort(dWidth))

  val masked_mem = Mem(size, Vec(4, UInt(8.W)))
  val nonmasked = Mem(size, UInt(32.W))

  val addr = (io.req.addr - (addr_offset >> 2).U)(15,0)

  val dataIn = Wire(Vec(4, UInt(8.W)))
  dataIn(0) := io.req.data(7,0)
  dataIn(1) := io.req.data(15,8)
  dataIn(2) := io.req.data(23,16)
  dataIn(3) := io.req.data(31,24)

  io.resp.data := 0.U
  io.resp.rdy := false.B

  when(io.req.valid) {
    when (io.req.wr) {
      if (wr_mask) {
        masked_mem.write(io.req.addr, dataIn, io.req.wr_mask)
      } else {
        nonmasked(addr) := io.req.data
      }
    } .otherwise {
      if (wr_mask) {
        io.resp.data := Cat(masked_mem.read(addr).reverse)

      } else {
        io.resp.data := nonmasked(io.req.addr)
      }
    }
    io.resp.rdy := true.B
  }
  if (wr_mask) {
    val read = masked_mem.read((size-1).U).asUInt
    io.succ := read(1, 0)
  } else {
    val read = nonmasked.read((size-1).U).asUInt
    io.succ := read(1, 0)
  }
}

