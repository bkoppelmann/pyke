package pyke

import chisel3._
import chisel3.util._

import Constants._

class ALUIO extends Bundle {
  val in1 = Input(UInt(16.W))
  val in2 = Input(UInt(16.W))
  val out = Output(UInt(16.W))
  val op  = Input(UInt(aluop_width.W))
}

class ALUSimple extends Module {

  val io = IO(new ALUIO)
  val shamt = io.in2(3,0)

  io.out := MuxLookup(io.op, 0.U,
          Seq(
            ALU_ADD  -> (io.in1 + io.in2),
            ALU_SUB  -> (io.in1 - io.in2),
            ALU_OR   -> (io.in1 | io.in2),
            ALU_AND  -> (io.in1 & io.in2),
            ALU_XOR  -> (io.in1 ^ io.in2),
            ALU_SLT  -> (io.in1.asSInt() < io.in2.asSInt()).asUInt(),
            ALU_SLTU -> (io.in1 < io.in2),
            ALU_SLL  -> (io.in1 << shamt)(15, 0),
            ALU_SRL  -> (io.in1 >> shamt),
            ALU_SRA  -> (io.in1.asSInt() >> shamt).asUInt()
          ))
}
