package org.omniai.mcp.gateway.parsing

import net.mamoe.yamlkt.Yaml
import org.omniai.mcp.dto.BrokerConfigDto

/**
 * Parses YAML strings into [BrokerConfigDto].
 */
class YamlConfigParser {
    private val yaml = Yaml {
        encodeDefaultValues = false
    }

    fun parse(content: String): BrokerConfigDto {
        return yaml.decodeFromString(BrokerConfigDto.serializer(), content)
    }
}
