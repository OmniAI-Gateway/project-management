package org.omniai.mcp.dto

import kotlinx.serialization.Serializable

/**
 * DTO for configuring an external REST API as an MCP Tool.
 */
@Serializable
data class ToolConfigDto(
    val name: String,
    val description: String? = null,
    val targetUrl: String,
    val method: String = "GET",
    /** Key-value pairs for headers (e.g. Authorization, Content-Type) */
    val headers: Map<String, String>? = null,
    /** JSON Schema for the input parameters */
    val inputSchema: Map<String, String>? = null
)
