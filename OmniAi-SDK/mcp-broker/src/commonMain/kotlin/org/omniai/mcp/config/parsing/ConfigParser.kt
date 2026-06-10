package org.omniai.mcp.config.parsing

import org.omniai.mcp.config.schema.ApiConfigDefinition

/**
 * Inbound port: parses a raw configuration string into [ApiConfigDefinition].
 *
 * Accepts a [String] rather than a file path to maintain KMP compatibility
 * (no `java.io.File` dependency). The actual file reading is handled by
 * platform-specific wrappers or an Okio implementation outside this module.
 */
interface ConfigParser {

    /**
     * Parses the given configuration content and returns the structured definition.
     *
     * @param content Raw configuration content (e.g., YAML string).
     * @return Parsed [ApiConfigDefinition] with tools and resources.
     * @throws IllegalArgumentException if the content is malformed.
     */
    fun parse(content: String): ApiConfigDefinition
}
