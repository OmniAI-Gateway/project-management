package org.omniai.mcp.domain

/**
 * Domain representation of a configured static Context in the Broker.
 */
data class BrokerContext(
    val name: String,
    val uri: String,
    val description: String?,
    val mimeType: String?,
    val content: String
)
