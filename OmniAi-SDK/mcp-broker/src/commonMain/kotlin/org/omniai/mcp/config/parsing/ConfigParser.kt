package org.omniai.mcp.config.parsing

import org.omniai.mcp.config.dto.BrokerConfigDto

/**
 * Interface for parsing configuration content into [BrokerConfigDto].
 */
interface ConfigParser {
    fun parse(content: String): BrokerConfigDto
}
