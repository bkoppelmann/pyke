package pyke

import chisel3._
import chisel3.util._
import config.YamlConfig

import Constants._

class ALUIO()(implicit config:YamlConfig) extends Bundle {
  val in1 = Input(UInt(16.W))
  val in2 = Input(UInt(16.W))
  val out = Output(UInt(16.W))
  val op  = Input(UInt(aluop_width.W))
}

class ALUSimple()(implicit config:YamlConfig) extends Module {

  val io = IO(new ALUIO)

  io.out := MuxLookup(io.op, 0.U,
          Seq(
            ALU_ADD  -> (io.in1 + io.in2),
            ALU_SUB  -> (io.in1 - io.in2),
            ALU_OR   -> (io.in1 | io.in2),
            ALU_AND  -> (io.in1 & io.in2),
            ALU_XOR  -> (io.in1 ^ io.in2),
            ALU_SLT  -> (io.in1.asSInt() < io.in2.asSInt()).asUInt(),
            ALU_SLTU -> (io.in1 < io.in2),
            ALU_SLL  -> (io.in1 << io.in2)(15, 0),
            ALU_SRL  -> (io.in1 >> io.in2),
            ALU_SRA  -> (io.in1.asSInt() >> io.in2).asUInt()
          ))
}

