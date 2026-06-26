package org.omniai.mcp.config.parsing

import net.mamoe.yamlkt.Yaml
import org.omniai.mcp.config.dto.BrokerConfigDto

/**
 * Parses YAML strings into [BrokerConfigDto].
 */
class YamlConfigParser : ConfigParser {
    private val yaml = Yaml {
        encodeDefaultValues = false
    }

    override fun parse(content: String): BrokerConfigDto {
        return yaml.decodeFromString(BrokerConfigDto.serializer(), content)
    }
}
