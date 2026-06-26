package org.omniai.mcp.domain.model

/**
 * Domain representation of a configured static Context in the Broker.
 */
data class ContextDefinition(
    val name: String,
    val uri: String,
    val description: String?,
    val mimeType: String?,
    val content: String
)
