package pyke

import chisel3.util.BitPat

object Instructions {
  def ADD                = BitPat("b????????????0000")
  def ADDI               = BitPat("b????????????0001")
  def SUB                = BitPat("b????????????0010")
  def LW                 = BitPat("b????????????0011")
  def SW                 = BitPat("b????????????0100")
  def SLL                = BitPat("b????????????0101")
  def SRL                = BitPat("b????????????0110")
  def SRA                = BitPat("b????????????0111")
  def XOR                = BitPat("b????????????1000")
  def AND                = BitPat("b????????????1001")
  def OR                 = BitPat("b????????????1010")
  def BEQ                = BitPat("b????????????1011")
  def BNE                = BitPat("b????????????1110")
  def JR                 = BitPat("b????????????1100")
  def SLLI               = BitPat("b????????????1101")
}
