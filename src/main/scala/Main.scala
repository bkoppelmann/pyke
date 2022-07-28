package pyke

import chisel3.stage.ChiselGeneratorAnnotation
import firrtl.options.TargetDirAnnotation
import config.ConfigParser

object Main extends App {

  require(args.size == 2, "Usage: sbt> run TargetDir ConfigPath")
  val targetDirectory = args.head
  val configName = args(1)
  val ymlConfig = ConfigParser.parse(configName)

  new chisel3.stage.ChiselStage().execute(
    args,
    Seq(
      ChiselGeneratorAnnotation(() =>
        new PykeTop(ymlConfig)
      ),
      TargetDirAnnotation(targetDirectory)
    )
  )
}
