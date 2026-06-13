package org.omniai.mcp.dto

import kotlinx.serialization.Serializable

/**
 * DTO for configuring static contexts, exposed as MCP Resources.
 * This allows providing static HTML, plain text, or other content.
 */
@Serializable
data class ContextConfigDto(
    val name: String,
    /** The URI this context will be exposed at, e.g. "context://my-page.html" */
    val uri: String,
    val description: String? = null,
    val mimeType: String? = "text/plain",
    /** The static content to provide, or a path/url to fetch from. For now, simple raw content. */
    val content: String
)
