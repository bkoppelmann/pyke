package config
import java.util.{List => JList, Map => JMap}
import java.io.FileReader
import collection.JavaConverters._
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.databind.ObjectMapper

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
  ) {

}

class YamlIsaConfig (@JsonProperty("atomLen") _atomLen: Int,
                     @JsonProperty("atomPerInsn") _atomPerInsn: Int,
                     @JsonProperty("xLen") _xLen: Int) {
}

class YamlDecoderConfig (@JsonProperty("imm_fields") _imm_fields: JList[String],
                         @JsonProperty("reg_fields") _reg_fields: JList[String],
                         @JsonProperty("label_fields") _label_fields: JList[String]) {
}
