package org.omniai.mcp.gateway.mapping

import org.omniai.mcp.domain.BrokerContext
import org.omniai.mcp.domain.BrokerServerClient
import org.omniai.mcp.domain.BrokerTool

/**
 * The result of mapping a [org.omniai.mcp.dto.BrokerConfigDto] to domain models.
 */
data class MappedConfig(
    val tools: List<BrokerTool>,
    val contexts: List<BrokerContext>,
    val servers: List<BrokerServerClient>
)
