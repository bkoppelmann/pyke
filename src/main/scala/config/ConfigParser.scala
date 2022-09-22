package config
import java.util.{List => JList, Map => JMap}
import java.io.FileReader
import collection.JavaConverters._
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.databind.ObjectMapper
import collection.JavaConverters._
import chisel3.util.log2Ceil

object ConfigParser {
  def parse(filepath: String):YamlConfig = {
     val reader = new FileReader(filepath)
     val mapper = new ObjectMapper(new YAMLFactory())
     val config: YamlConfig = mapper.readValue(reader, classOf[YamlConfig])
     config
  }
}

class YamlConfig(@JsonProperty("isa") _isa: YamlIsaConfig,
                 @JsonProperty("decoder") _decoder: YamlDecoderConfig,
                 @JsonProperty("hw") _hw: YamlHWConfig,
  ) {
  require(_isa != null, "yaml: isa needs to be defined")
  require(_decoder != null, "yaml: decoder needs to be defined")
  require(_hw != null, "yaml: hw needs to be defined")
  val isa = _isa
  val decoder = _decoder
  val hw = _hw
}

class YamlIsaConfig (@JsonProperty("atomLen") _atomLen: Int,
                     @JsonProperty("atomPerInsn") _atomPerInsn: Int,
                     @JsonProperty("xLen") _xLen: Int,
                     @JsonProperty("numRegsWithZero") _numRegsWithZero: Int) {
  val atomLen = _atomLen
  val pcIncr = _atomLen / 8
  val atomsPerInsn = _atomPerInsn
  val insnLen = atomLen * atomsPerInsn
  val insnLenBytes = insnLen / 8
  val xLen = _xLen
  val numRegs = _numRegsWithZero-1
  val regAddrSize = log2Ceil(_numRegsWithZero)
}

class YamlDecoderConfig (@JsonProperty("imm_fields") _imm_fields: JList[String],
                         @JsonProperty("reg_src_fields") _reg_src_fields: JList[String],
                         @JsonProperty("reg_dst_fields") _reg_dst_fields: JList[String],
                         @JsonProperty("label_fields") _label_fields: JList[String]) {
  require(_imm_fields != null, "yaml: imm_fields needs to be defined")
  require(_reg_src_fields != null, "yaml: reg_src_fields needs to be defined")
  require(_reg_dst_fields != null, "yaml: reg_dst_fields needs to be defined")
  val immFields = _imm_fields
  val regSrcFields = _reg_src_fields
  val regDstFields = _reg_dst_fields
}

class YamlHWConfig (@JsonProperty("lanes") _lanes: JList[String]) {
  require(_lanes != null, "yaml: lanes needs to be defined")
  val lanes = _lanes.asScala
}
