package pyke

import chisel3._
import chisel3.util._

import Constants._

class LaneIO extends Bundle {
  val insn = Input(UInt(16.W))
  val pc = Input(UInt(16.W))
  val pc_next = Input(UInt(16.W))
}

class Lane extends Module {
  val io = IO(new LaneIO)

  val decoder = Module(new Decoder)
  val ctrl = decoder.io.ctrl

  val rf = Module(new RegisterFile)
  val alu = Module(new ALUSimple)

  val rd_addr  = io.insn(7,4)
  val rs1_addr = io.insn(11,8)
  val rs2_addr = io.insn(15,12)

  // Decode
  decoder.io.insn := io.insn

  // RF access
  rf.io.r_addr1 := rs1_addr
  rf.io.r_addr2 := rs2_addr
  rf.io.en      := (ctrl.op1_sel === OP1_RS1 || ctrl.op2_sel === OP2_RS2)

  val rs1 = rf.io.r_data1
  val rs2 = rf.io.r_data2

  // Execute
  val inp1 = MuxLookup(ctrl.op1_sel, 0.U,
    Seq(
      OP1_RS1  -> rs1,
      OP1_PC   -> io.pc,
      OP1_IMM  -> rs1_addr
      ))
  alu.io.in1 := inp1

  val inp2 = MuxLookup(ctrl.op2_sel, 0.U,
    Seq(
      OP2_RS2   -> rs2,
      OP2_IMM_I -> 0.U,
      OP2_IMM_U -> 0.U,
      OP2_IMM_S -> 0.U
      ))
  alu.io.in2 := inp2
  alu.io.op := ctrl.alu_op

  // WB
  val wb = MuxCase(0.U,
                Seq(
                  (ctrl.wb_sel === WB_ALU) -> alu.io.out,
                  (ctrl.wb_sel === WB_MEM) -> 0.U,
                  (ctrl.wb_sel === WB_PC4) -> io.pc_next,
                ))
  rf.io.wr_addr := rd_addr
  rf.io.wr_data := wb
  rf.io.wen := ctrl.wr_reg

}
