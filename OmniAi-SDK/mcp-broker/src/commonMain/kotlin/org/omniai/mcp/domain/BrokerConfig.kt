package org.omniai.mcp.domain

import org.omniai.mcp.domain.model.ContextDefinition
import org.omniai.mcp.domain.model.McpClientConfig
import org.omniai.mcp.domain.model.RestToolDefinition

/**
 * The aggregated broker configuration after parsing and mapping.
 */
data class BrokerConfig(
    val tools: List<RestToolDefinition>,
    val contexts: List<ContextDefinition>,
    val mcpClients: List<McpClientConfig>,
)
