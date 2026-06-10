package org.omniai.mcp.config.schema

import kotlinx.serialization.Serializable

/**
 * Top-level YAML configuration root.
 * Represents the entire dynamic API configuration file containing
 * tool and resource definitions.
 */
@Serializable
data class ApiConfigDefinition(
    val tools: List<ToolDefinition> = emptyList(),
    val resources: List<ResourceDefinition> = emptyList()
)
