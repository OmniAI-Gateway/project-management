package org.omniai.mcp.domain

import kotlinx.serialization.json.JsonObject

/**
 * Domain representation of a configured Tool in the Broker.
 */
data class BrokerTool(
    val name: String,
    val description: String?,
    val targetUrl: String,
    val method: String,
    val headers: Map<String, String>,
    val pathSchema: JsonObject?,
    val querySchema: JsonObject?,
    val bodySchema: JsonObject?
)
