package org.omniai.mcp.dto

import kotlinx.serialization.Serializable

/**
 * DTO for configuring static contexts, exposed as MCP Resources.
 * This allows providing static HTML, plain text, or other content.
 */
@Serializable
data class ContextConfigDto(
    val name: String,
    val uri: String,
    val description: String? = null,
    val mimeType: String? = "text/plain",
    val content: String
)
