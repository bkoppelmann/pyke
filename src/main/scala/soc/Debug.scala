package soc

import chisel3._
import chisel3.util.Cat

class DebugOffChipIO extends Bundle {
    val imem_addr = Input(UInt(32.W))
    val imem_val  = Input(UInt(32.W))
    val fetch_en  = Input(Bool())
}
class DebugIO() extends Bundle {
    val off = new DebugOffChipIO()
    val imem = Flipped(new ScratchPadPort(32))
    val cpu_fetch_en = Output(Bool())
}

class DebugModule() extends Module {
    val io = IO(new DebugIO())

    io.cpu_fetch_en := io.off.fetch_en

    io.imem.req.addr      := io.off.imem_addr(31,2)
    io.imem.req.valid     := true.B
    io.imem.req.wr        := true.B
    io.imem.req.wr_mask   := Seq(true.B, true.B, true.B, true.B)
    io.imem.req.data      := io.off.imem_val

    io.imem.resp := DontCare
}

