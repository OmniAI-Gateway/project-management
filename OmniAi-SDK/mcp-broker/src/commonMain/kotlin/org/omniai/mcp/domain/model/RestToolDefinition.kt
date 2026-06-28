package org.omniai.mcp.domain.model

import kotlinx.serialization.json.JsonObject

/**
 * Domain representation of a REST API tool definition.
 */
data class RestToolDefinition(
    val name: String,
    val description: String?,
    val targetUrl: String,
    val method: String,
    val headers: Map<String, String>,
    val pathSchema: JsonObject?,
    val querySchema: JsonObject?,
    val bodySchema: JsonObject?,
)
