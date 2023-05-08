package soc

import chisel3._
import chisel3.util.{Cat, log2Ceil}
import config.YamlConfig

class DebugOffChipIO()(implicit config:YamlConfig) extends Bundle {
    val imem_addr = Input(UInt(config.isa.xLen.W))
    val imem_val  = Input(UInt(8.W))
    val fetch_en  = Input(Bool())
}
class DebugIO()(implicit config:YamlConfig) extends Bundle {
    val off = new DebugOffChipIO()
    val imem = Flipped(new ScratchPadPort(config.isa.xLen, config.isa.insnLen))
    val cpu_fetch_en = Output(Bool())
}

class DebugModule()(implicit config:YamlConfig) extends Module {
    val io = IO(new DebugIO())

    val deser = Module(new Deserializer(inputSize=8, outputSize=config.isa.insnLenBits))
    deser.io.in := io.off.imem_val.asUInt

    io.cpu_fetch_en := io.off.fetch_en

    io.imem.req.addr      := io.off.imem_addr >> log2Ceil(config.isa.insnLenBytes)
    io.imem.req.valid     := deser.io.valid & ~io.cpu_fetch_en
    io.imem.req.wr        := true.B
    io.imem.req.wr_mask   := Seq.fill(config.isa.insnLen / 8)(true.B)
    io.imem.req.data      := deser.io.out

    io.imem.resp := DontCare
}

