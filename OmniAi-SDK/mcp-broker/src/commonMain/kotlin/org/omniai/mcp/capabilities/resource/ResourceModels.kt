package org.omniai.mcp.capabilities.resource

data class ResourceContent(
    val uri: String,
    val mimeType: String? = null,
    val text: String? = null,
    val blob: String? = null // Base64 encoded
)
