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
    val headers: Map<String, String>? = null,
    val inputSchema: Map<String, String>? = null
)
