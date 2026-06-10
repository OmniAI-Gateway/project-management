package org.omniai.mcp.config.parsing

import net.mamoe.yamlkt.Yaml
import org.omniai.mcp.config.schema.ApiConfigDefinition

/**
 * yamlkt-based implementation of [ConfigParser].
 *
 * Uses `net.mamoe.yamlkt` with `kotlinx.serialization` to deserialize
 * YAML strings into [ApiConfigDefinition]. Fully KMP-compatible.
 */
class YamlConfigParser : ConfigParser {

    private val yaml = Yaml.Default

    override fun parse(content: String): ApiConfigDefinition {
        return yaml.decodeFromString(ApiConfigDefinition.serializer(), content)
    }
}
