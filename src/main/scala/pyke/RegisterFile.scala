package pyke

import chisel3._

class RFReadPortIO(dataSize:Int, addrSize:Int) extends Bundle {
  val addr = Input(UInt(addrSize.W))
  val data = Output(UInt(dataSize.W))
  val en = Input(Bool())
}

class RFWritePortIO(dataSize:Int, addrSize:Int) extends Bundle {
  val addr = Input(UInt(addrSize.W))
  val data = Input(UInt(dataSize.W))
  val en = Input(Bool())
}


class RegisterFilePort(numReadPorts:Int, numWritePorts:Int) extends Bundle {
  val read_ports = Vec(numReadPorts, new RFReadPortIO(16, 4))
  val write_ports = Vec(numWritePorts, new RFWritePortIO(16, 4))
}

class RegisterFile(numReadPorts:Int, numWritePorts:Int) extends Module {
  val io:RegisterFilePort = IO(new RegisterFilePort(numReadPorts, numWritePorts))

  // x0 is hardwired to 0 so we don't allocate it.
  // Thus x1 is equal to mem(0), x2 is equal to mem(1) and so on..
  val mem = Mem(15, UInt(16.W))

  // write ports
  for (i <- 0 until numWritePorts) {
    when (io.write_ports(i).en && io.write_ports(i).addr =/= 0.U) {
      mem(io.write_ports(i).addr - 1.U) := io.write_ports(i).data
    }
  }

  // Read ports
  for (i <- 0 until numReadPorts) {
    io.read_ports(i).data := 0.U
    when (io.read_ports(i).en && io.read_ports(i).addr =/= 0.U) {
      io.read_ports(i).data := mem(io.read_ports(i).addr - 1.U)
    }
  }
}
