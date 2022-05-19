package pyke

import chisel3._
import chisel3.util._

import Constants._
import Instructions._

class CtrlSignals extends Bundle {
  val valid: Bool    = Bool()
  val op1_sel: UInt  = UInt(op1_width.W)
  val op2_sel: UInt  = UInt(op2_width.W)
  val alu_op: UInt   = UInt(aluop_width.W)
  val wr_reg: Bool   = Bool()
  val mem_op: UInt   = UInt(memop_width.W)
  val mem_size: UInt = UInt(memsize_width.W)
  val br:UInt        = UInt(branch_width.W)
  val wb_sel:UInt    = UInt(wb_width.W)
}


class DecoderIO extends Bundle {
  val insn = Input(UInt(16.W))
  val ctrl = Output(new CtrlSignals)
}

class Decoder extends Module {
  val io = IO(new DecoderIO)

  val ctrlSignals = ListLookup(io.insn, 
                 //                OP1                         writeback en
               // valid           |                            |        mem operation
               //   |  branch     |       OP2        ALU_OP    | writeback |       mem size
               //   |   type      |        |           |       |  select   |         |  
               //   |    |        |        |           |       |    |      |         |      
/* default ->*/List(N, BR_N,    OP1_X,    OP2_X,     ALU_X,    N, WB_X  , MEM_X,     0.U),
  Array(
      ADD   -> List(Y, BR_N,    OP1_RS1,  OP2_RS2,   ALU_ADD,  Y, WB_ALU, MEM_X,     0.U),
      ADDI  -> List(Y, BR_N,    OP1_RS1,  OP2_IMM_I, ALU_ADD,  Y, WB_ALU, MEM_X,     0.U),
      SUB   -> List(Y, BR_N,    OP1_RS1,  OP2_RS2,   ALU_SUB,  Y, WB_ALU, MEM_X,     0.U),
      LW    -> List(Y, BR_N,    OP1_RS1,  OP2_IMM_I, ALU_ADD,  Y, WB_ALU, MEM_LOAD,  WORD),
      SW    -> List(Y, BR_N,    OP1_RS1,  OP2_IMM_I, ALU_ADD,  N, WB_X,   MEM_STORE, WORD),
      SLL   -> List(Y, BR_N,    OP1_RS1,  OP2_RS2,   ALU_SLL,  Y, WB_ALU, MEM_X,     0.U),
      SRL   -> List(Y, BR_N,    OP1_RS1,  OP2_RS2,   ALU_SRL,  Y, WB_ALU, MEM_X,     0.U),
      SRA   -> List(Y, BR_N,    OP1_RS1,  OP2_RS2,   ALU_SRA,  Y, WB_ALU, MEM_X,     0.U),
      XOR   -> List(Y, BR_N,    OP1_RS1,  OP2_RS2,   ALU_XOR,  Y, WB_ALU, MEM_X,     0.U),
      AND   -> List(Y, BR_N,    OP1_RS1,  OP2_RS2,   ALU_AND,  Y, WB_ALU, MEM_X,     0.U),
      OR    -> List(Y, BR_N,    OP1_RS1,  OP2_RS2,   ALU_OR ,  Y, WB_ALU, MEM_X,     0.U),
      OR    -> List(Y, BR_N,    OP1_RS1,  OP2_RS2,   ALU_OR ,  Y, WB_ALU, MEM_X,     0.U),
      BEQ   -> List(Y, BR_EQ,   OP1_RS1,  OP2_X,     ALU_X,    N, WB_X,   MEM_X,     0.U),
      JR    -> List(Y, BR_JALR, OP1_RS1,  OP2_IMM_I, ALU_ADD,  Y, WB_PC4, MEM_X,     0.U),
  ))

  io.ctrl.mem_size := ctrlSignals(8)
  io.ctrl.mem_op   := ctrlSignals(7)
  io.ctrl.wb_sel   := ctrlSignals(6)
  io.ctrl.wr_reg   := ctrlSignals(5)
  io.ctrl.alu_op   := ctrlSignals(4)
  io.ctrl.op2_sel  := ctrlSignals(3)
  io.ctrl.op1_sel  := ctrlSignals(2)
  io.ctrl.br       := ctrlSignals(1)
  io.ctrl.valid    := ctrlSignals.head
}
