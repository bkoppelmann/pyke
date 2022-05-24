package pyke

import chisel3._
import chisel3.util._

import soc.ScratchPadPort

import Constants._

class LaneIO extends Bundle {
  val insn = Input(UInt(16.W))
  val pc = Input(UInt(32.W))
  val pc_plus4 = Input(UInt(32.W))
  val pc_next = Output(UInt(32.W))
  val dmem = Flipped(new ScratchPadPort(32))
}

class Lane(has_bru:Boolean) extends Module {
  val io = IO(new LaneIO)

  val decoder = Module(new Decoder)
  val ctrl = decoder.io.ctrl

  val rf = Module(new RegisterFile)
  val alu = Module(new ALUSimple)
  val lsu = Module(new LoadStoreUnit)


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

  if (has_bru) {
    val bru = Module(new BranchUnit)
    io.pc_next := Mux(ctrl.br =/= BR_N, bru.io.out, io.pc_plus4)

    bru.io.br_target := io.pc + io.insn(7,4)
    bru.io.jal_target := rs1 + io.insn(15,12)
    bru.io.rs1 := rs1
    bru.io.rs2 := rs2
    bru.io.pc_plus4 := io.pc_plus4
    bru.io.ctrl := ctrl.br
  } else {
    io.pc_next := DontCare
  }

  // MEM
  lsu.io.dmem <> io.dmem
  lsu.io.en := (ctrl.mem_op =/= MEM_X)
  lsu.io.wr := (ctrl.mem_op === MEM_STORE)
  lsu.io.addr := (alu.io.out)
  lsu.io.wr_data := rs2

  // WB
  val wb = MuxCase(0.U,
                Seq(
                  (ctrl.wb_sel === WB_ALU) -> alu.io.out,
                  (ctrl.wb_sel === WB_MEM) -> lsu.io.r_data,
                  (ctrl.wb_sel === WB_PC4) -> io.pc_next,
                ))
  rf.io.wr_addr := rd_addr
  rf.io.wr_data := wb
  rf.io.wen := ctrl.wr_reg

}
