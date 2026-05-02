package org.omniai.mcp.capabilities.tool

import kotlinx.serialization.json.JsonObject
import org.omniai.mcp.capabilities.resource.ResourceContent

data class ToolResult(
    val content: List<ToolContent>,
    val isError: Boolean = false,
    val meta: JsonObject? = null
)

sealed interface ToolContent {
    data class Text(val text: String) : ToolContent
    data class Image(val data: String, val mimeType: String) : ToolContent
    data class Resource(val resource: ResourceContent) : ToolContent
}

data class ToolSchemaDefinition(
    val properties: JsonObject?,
    val required: List<String>?
)
