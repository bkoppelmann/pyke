package pyke

import chisel3._

class RegisterFilePort extends Bundle {
  val wen:Bool = Input(Bool())
  val en:Bool  = Input(Bool())

  val r_addr1  = Input(UInt(4.W))
  val r_addr2  = Input(UInt(4.W))
  val wr_addr  = Input(UInt(4.W))

  val r_data1  = Output(UInt(16.W))
  val r_data2  = Output(UInt(16.W))
  val wr_data  = Input(UInt(16.W))

}

class RegisterFile extends Module {
  val io:RegisterFilePort = IO(new RegisterFilePort)

  // if io_r_addr1 === 0
  io.r_data1 := 0.U
  // if io_r_addr2 === 0
  io.r_data2 := 0.U
  // x0 is hardwired to 0 so we don't allocate it.
  // Thus x1 is equal to mem(0), x2 is equal to mem(1) and so on..
  val mem = Mem(15, UInt(16.W))

  // write port
  when (io.wen && io.wr_addr =/= 0.U) {
    mem(io.wr_addr - 1.U) := io.wr_data
  }

  // read port1
  when (io.en && io.r_addr1 =/= 0.U) {
    io.r_data1 := mem(io.r_addr1 - 1.U)
  }

  // read port2
  when (io.en && io.r_addr2 =/= 0.U) {
    io.r_data2 := mem(io.r_addr2 - 1.U)
  }

}
