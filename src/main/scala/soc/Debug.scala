package soc

import chisel3._
import chisel3.util.Cat
import config.YamlConfig

class DebugOffChipIO()(implicit config:YamlConfig) extends Bundle {
    val imem_addr = Input(UInt(config.isa.xLen.W))
    val imem_val  = Input(UInt(config.isa.insnLen.W))
    val fetch_en  = Input(Bool())
}
class DebugIO()(implicit config:YamlConfig) extends Bundle {
    val off = new DebugOffChipIO()
    val imem = Flipped(new ScratchPadPort(config.isa.xLen, config.isa.insnLen))
    val cpu_fetch_en = Output(Bool())
}

class DebugModule()(implicit config:YamlConfig) extends Module {
    val io = IO(new DebugIO())

    io.cpu_fetch_en := io.off.fetch_en

    io.imem.req.addr      := io.off.imem_addr(31,2)
    io.imem.req.valid     := true.B
    io.imem.req.wr        := true.B
    io.imem.req.wr_mask   := Seq.fill(config.isa.insnLen / 8)(true.B)
    io.imem.req.data      := io.off.imem_val

    io.imem.resp := DontCare
}

