package pyke

import chisel3._
import chisel3.util._

import soc.ScratchPadPort
import config.YamlConfig

import Constants._

class LaneIO()(implicit config:YamlConfig) extends Bundle {
  val insn = Input(UInt(config.isa.atomLen.W))
  val pc = Input(UInt(32.W))
  val pc_plus4 = Input(UInt(32.W))
  val pc_next = Output(UInt(32.W))
  val dmem = Flipped(new ScratchPadPort(32, 32))
  val rfReadPorts = Flipped(Vec(2, new RFReadPortIO(16, 4)))
  val rfWritePort = Flipped(new RFWritePortIO(16, 4))
}

object Lane {

  def createFromName(name:String)(implicit config:YamlConfig) : Option[Lane] = {
    name match {
      case "ALULane" => Some(ALULane(config))
      case "MemALULane" => Some(MemALULane(config))
      case "BranchALULane" => Some(BranchMemALULane(config))
      case "BranchMemALULane" => Some(BranchMemALULane(config))
      case _ =>  None
    }
  }
}

object ALULane {
  def apply(implicit config:YamlConfig) : Lane = {
    val res = Module(new Lane(false, false))
    res.io.dmem := DontCare
    res
  }
}

object MemALULane {
  def apply(implicit config:YamlConfig) : Lane = {
    println("Create MemALU")
    Module(new Lane(false, true))
  }
}

object BranchALULane {
  def apply(implicit config:YamlConfig) : Lane = {
    println("Create BranchALU")
    val res = Module(new Lane(true, false))
    res.io.dmem := DontCare
    res
  }
}

object BranchMemALULane {
  def apply(implicit config:YamlConfig) : Lane = {
    val res = Module(new Lane(true, true))
    res
  }
}

import InsnFieldExtractor._

class Lane(has_bru:Boolean, has_lsu:Boolean)(implicit config:YamlConfig) extends Module {
  val io = IO(new LaneIO)

  val decoder = Module(new Decoder)
  val ctrl = decoder.io.ctrl

  val alu = Module(new ALUSimple)

  val rd_addr  = InsnFieldExtractor.getDstRegAddr(io.insn, 0)
  val rs1_addr = InsnFieldExtractor.getSrcRegAddr(io.insn, 0)
  val rs2_addr = InsnFieldExtractor.getSrcRegAddr(io.insn, 1)
  val s_imm = rd_addr

  // Decode
  decoder.io.insn := io.insn

  // RF access
  io.rfReadPorts(0).addr := rs1_addr
  io.rfReadPorts(1).addr := rs2_addr

  io.rfReadPorts(0).en      := (ctrl.op1_sel === OP1_RS1 || ctrl.op2_sel === OP2_RS2)
  io.rfReadPorts(1).en      := (ctrl.op1_sel === OP1_RS1 || ctrl.op2_sel === OP2_RS2)

  val rs1 = io.rfReadPorts(0).data
  val rs2 = io.rfReadPorts(1).data

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
      OP2_IMM_I -> rs2_addr,
      OP2_IMM_S -> s_imm,
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
  val lsu_data = Wire(UInt(16.W))

  if (has_lsu) {
    val lsu = Module(new LoadStoreUnit)
    lsu.io.dmem <> io.dmem
    lsu.io.en := (ctrl.mem_op =/= MEM_X)
    lsu.io.wr := (ctrl.mem_op === MEM_STORE)
    lsu.io.addr := alu.io.out
    lsu.io.wr_data := rs2
    lsu_data := lsu.io.r_data
  } else {
    io.dmem := DontCare
    lsu_data := 0.U
  }
  // WB
  val wb = MuxCase(0.U,
                Seq(
                  (ctrl.wb_sel === WB_ALU) -> alu.io.out,
                  (ctrl.wb_sel === WB_MEM) -> lsu_data,
                  (ctrl.wb_sel === WB_PC4) -> io.pc_next,
                ))

  io.rfWritePort.addr := rd_addr
  io.rfWritePort.data := wb
  io.rfWritePort.en   := ctrl.wr_reg


  def has_bru() : Boolean = has_bru
  def has_lsu() : Boolean = has_lsu
}
