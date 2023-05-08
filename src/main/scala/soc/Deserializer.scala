package soc

import chisel3._
import chisel3.util.Reverse
import chisel3.util.log2Ceil

class Deserializer(inputSize:Int, outputSize:Int) extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(inputSize.W))
    val out = Output(UInt(outputSize.W))
    val valid = Output(Bool())
  })

  assert(outputSize > inputSize)
  assert(outputSize % inputSize == 0)
  val bufElements = outputSize / inputSize

  val buf = Reg(Vec(bufElements, UInt(inputSize.W)))
  val counter = RegInit(0.U(log2Ceil(bufElements).W))
  counter := counter + 1.U
  io.valid := counter === 0.U
  buf(counter) := io.in
  io.out := buf.asUInt
}
