package pyke

import chisel3._
import chisel3.util._

import Constants._

class BranchUnitIO extends Bundle {
  val rs1 = Input(UInt(16.W))
  val rs2 = Input(UInt(16.W))
  val pc_plus4  = Input(UInt(32.W))
  val ctrl = Input(UInt(branch_width.W))
  val br_target = Input(UInt(32.W))
  val jal_target = Input(UInt(32.W))
  val out = Output(UInt(32.W))
}

class BranchUnit extends Module {
  val io = IO(new BranchUnitIO)

  /*
   * conditions for
   *  beq/bge/bgeu/blt/bltu instructions
   */
  val br_eq  = io.rs1 === io.rs2
  val br_ne  = io.rs1 =/= io.rs2

  io.out := MuxCase(io.pc_plus4,
                Seq(
                  (io.ctrl === BR_EQ)   -> Mux(br_eq,  io.br_target,  io.pc_plus4),
                  (io.ctrl === BR_NE)   -> Mux(br_ne,  io.br_target,  io.pc_plus4),
                  (io.ctrl === BR_JALR) -> io.jal_target
                ))
}

