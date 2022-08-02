package soc

import chisel3._
import chisel3.util.Cat
import config.YamlConfig

class ScratchPadRequest(addrWidth:Int, dataWidth:Int) extends Bundle {
  val addr: UInt = Input(UInt(addrWidth.W))
  val data: UInt = Input(UInt(dataWidth.W))
  val wr: Bool = Input(Bool())
  val wr_mask = Vec(dataWidth/8, Input(Bool()))
  val valid: Bool = Input(Bool())
}

class ScratchPadResponse(dataWidth:Int) extends Bundle {
  val data: UInt = Output(UInt(dataWidth.W))
  val rdy: Bool = Output(Bool())
}

class ScratchPadPort(addrWidth:Int, dataWidth:Int) extends Bundle {
  val req = new ScratchPadRequest(addrWidth, dataWidth)
  val resp = new ScratchPadResponse(dataWidth)
  val succ: UInt = Output(UInt(2.W))
}

class ScratchPad(size:Int, addrWidth:Int, dataWidth:Int, addr_offset:BigInt, wr_mask:Boolean) extends Module {
  assert(dataWidth % 8 == 0)
  val io: ScratchPadPort = IO(new ScratchPadPort(addrWidth, dataWidth))

  val numBytes = dataWidth/8
  val masked_mem = Mem(size, Vec(numBytes, UInt(8.W)))
  val nonmasked = Mem(size, UInt(dataWidth.W))

  val addr = (io.req.addr - (addr_offset >> 2).U)(15,0)

  val inputBytes = splitInputToBytes()

  io.resp.data := 0.U
  io.resp.rdy := false.B

  when(io.req.valid) {
    when (io.req.wr) {
      if (wr_mask) {
        masked_mem.write(io.req.addr, inputBytes, io.req.wr_mask)
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

  def splitInputToBytes() : Vec[UInt] = {
    val res = Wire(Vec(numBytes, UInt(8.W)))
    var byte = 0
    for (byte <- 0 until numBytes) {
      val h = (byte+1) * 8 - 1
      val l = byte * 8
      res(byte) := io.req.data(h, l)
    }
    res
  }
}

