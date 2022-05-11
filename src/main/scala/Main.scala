package pyke

import chisel3.stage.ChiselGeneratorAnnotation
import firrtl.options.TargetDirAnnotation

object Main extends App {
  val targetDirectory = args.head
  new chisel3.stage.ChiselStage().execute(
    args,
    Seq(
      ChiselGeneratorAnnotation(() =>
        new PykeTop
      ),
      TargetDirAnnotation(targetDirectory)
    )
  )
}
