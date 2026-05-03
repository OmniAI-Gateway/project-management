package org.omniai.mcp.capabilities.resource

class McpResource(
    val uri: String,
    val name: String,
    val description: String? = null,
    val mimeType: String? = null,
    val handler: suspend (uri: String) -> ResourceContent
)
