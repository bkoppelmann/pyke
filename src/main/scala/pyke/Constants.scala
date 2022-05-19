package pyke

import chisel3._

// Constants for the decoder
trait ScalarOpConstants
{
  val op1_width = 2
  val op2_width = 3

  val N: Bool = false.B
  val Y: Bool = true.B

  // operands
  val OP1_X: UInt     = 0.asUInt(op1_width.W)
  val OP1_RS1: UInt   = 1.asUInt(op1_width.W)
  val OP1_PC : UInt   = 2.asUInt(op1_width.W)
  val OP1_IMM : UInt  = 3.asUInt(op1_width.W)

  val OP2_X: UInt    =  0.asUInt(op2_width.W)
  val OP2_RS2: UInt  =  1.asUInt(op2_width.W)
  val OP2_IMM_I: UInt = 2.asUInt(op2_width.W)
  val OP2_IMM_U: UInt = 3.asUInt(op2_width.W)
  val OP2_IMM_S: UInt = 4.asUInt(op2_width.W)

}

trait ALUConstants {
  val aluop_width = 4
  val ALU_X: UInt    = 0.asUInt(aluop_width.W)
  val ALU_ADD: UInt  = 1.asUInt(aluop_width.W)
  val ALU_SUB: UInt  = 2.asUInt(aluop_width.W)
  val ALU_AND: UInt  = 3.asUInt(aluop_width.W)
  val ALU_OR:  UInt  = 4.asUInt(aluop_width.W)
  val ALU_XOR: UInt  = 5.asUInt(aluop_width.W)
  val ALU_SLT: UInt  = 6.asUInt(aluop_width.W)
  val ALU_SLTU: UInt = 7.asUInt(aluop_width.W)
  val ALU_SLL: UInt  = 8.asUInt(aluop_width.W)
  val ALU_SRL: UInt  = 9.asUInt(aluop_width.W)
  val ALU_SRA: UInt  = 10.asUInt(aluop_width.W)
}

trait MemOps {
  val memop_width = 2
  val MEM_X: UInt          = 0.asUInt(memop_width.W)
  val MEM_LOAD: UInt       = 1.asUInt(memop_width.W)
  val MEM_LOAD_SEXT: UInt  = 2.asUInt(memop_width.W)
  val MEM_STORE: UInt      = 3.asUInt(memop_width.W)

}

trait LoadStoreConstants {
  val memsize_width = 2

  val BYTE     = 0.asUInt(memsize_width.W)
  val HALFWORD = 1.asUInt(memsize_width.W)
  val WORD     = 2.asUInt(memsize_width.W)

}

trait BranchConstants {
val branch_width = 4

val BR_N     = 0.asUInt(branch_width.W)
val BR_EQ    = 1.asUInt(branch_width.W)
val BR_NE    = 2.asUInt(branch_width.W)
val BR_LT    = 3.asUInt(branch_width.W)
val BR_LTU   = 4.asUInt(branch_width.W)
val BR_GE    = 5.asUInt(branch_width.W)
val BR_GEU   = 6.asUInt(branch_width.W)
val BR_JAL   = 7.asUInt(branch_width.W)
val BR_JALR  = 8.asUInt(branch_width.W)

}

trait WbSelect {
val wb_width = 3

val WB_X     = 0.asUInt(wb_width.W)
val WB_ALU   = 1.asUInt(wb_width.W)
val WB_MEM   = 2.asUInt(wb_width.W)
val WB_PC4   = 3.asUInt(wb_width.W)
val WB_CSR   = 4.asUInt(wb_width.W)

}

trait CSRCommands {
  val csr_width = 2

  val CSR_N = 0.asUInt(csr_width.W)
  val CSR_W = 1.asUInt(csr_width.W)
  val CSR_S = 2.asUInt(csr_width.W)
  val CSR_C = 3.asUInt(csr_width.W)
}


trait InsnConstants {
  val NOP: UInt = 0x0.U(32.W)    // this is xor x0, x0, x0. The compiler emits
                                 // add x0, x0, x0 which allows us to
                                 // easily identify NOPs inserted by the core
}

object Constants extends ScalarOpConstants
  with InsnConstants
  with ALUConstants
  with MemOps
  with LoadStoreConstants
  with BranchConstants
  with WbSelect
  with CSRCommands

