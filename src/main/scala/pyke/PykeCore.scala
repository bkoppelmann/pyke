package pyke

import chisel3._
import chisel3.util._
import soc.ScratchPadPort
import config.YamlConfig

import Constants._

class CoreIO()(implicit config:YamlConfig) extends Bundle {
  val imem = Flipped(new ScratchPadPort(config.isa.xLen, config.isa.insnLen))
  val dmem = Flipped(new ScratchPadPort(config.isa.xLen, config.isa.xLen))
  val fetch_en = Input(Bool())
}

class PykeCore()(implicit config:YamlConfig) extends Module {
  val io: CoreIO = IO(new CoreIO())

  val numReadPorts = config.isa.atomsPerInsn * 2
  val numWritePorts = config.isa.atomsPerInsn
  val rf    = Module(new RegisterFile(numReadPorts, numWritePorts)) // 2 readPorts per Lane, 1 writePort per Lane times 2 Lanes
  var lanes :List[Lane] = List()

  val pc = RegInit(0x8000L.U(config.isa.xLen.W))
  val pc_plusX = pc + config.isa.pcIncr.U

 /*
  * fetch
  */
 io.imem.req.addr    := pc >> log2Ceil(config.isa.insnLenBytes)
  io.imem.req.valid   := true.B
  io.imem.req.wr      := false.B
  io.imem.req.wr_mask := DontCare
  io.imem.req.data    := DontCare

  val insn = Mux(io.imem.resp.rdy, io.imem.resp.data, NOP)
  val zippedLanes = config.hw.lanes.zipWithIndex
  zippedLanes.foreach(createAndConnectLane)

  def createAndConnectLane(laneIndex: (String, Int)):Unit = {
    val (name, index) = laneIndex

    Lane.createFromName(name) match {
      case Some(lane) => {

        lane.suggestName(s"lane_$index")
        lane.io.insn := Mux(!io.fetch_en, NOP, extractAtom(index, insn))
        lanes :+ lane
        lane.io.pc := pc
        lane.io.pc_plusX := Mux(!io.fetch_en, pc, pc_plusX)

        lane.io.rfReadPorts(0) <> rf.io.read_ports(index * 2)
        lane.io.rfReadPorts(1) <> rf.io.read_ports(index * 2 + 1)
        lane.io.rfWritePort <> rf.io.write_ports(index)

        if (lane.has_lsu()) {
          lane.io.dmem <> io.dmem
        }

        if (lane.has_bru()) {
          pc := lane.io.pc_next
        }
      }
      case None => println(s"Error: Unknown Lanetype '$name'")
    }
  }

  def extractAtom(index:Int, insn:UInt) :UInt = {
        val start = config.isa.atomLen * index
        val end = start + config.isa.atomLen - 1
        insn(end, start)
  }
}
