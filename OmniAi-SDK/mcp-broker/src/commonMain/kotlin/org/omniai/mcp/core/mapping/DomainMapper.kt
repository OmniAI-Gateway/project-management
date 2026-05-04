package org.omniai.mcp.core.mapping

import io.modelcontextprotocol.kotlin.sdk.types.BlobResourceContents
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ContentBlock
import io.modelcontextprotocol.kotlin.sdk.types.EmbeddedResource
import io.modelcontextprotocol.kotlin.sdk.types.ImageContent
import io.modelcontextprotocol.kotlin.sdk.types.Prompt
import io.modelcontextprotocol.kotlin.sdk.types.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.types.Resource
import io.modelcontextprotocol.kotlin.sdk.types.Role
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import org.omniai.mcp.capabilities.prompt.McpPrompt
import org.omniai.mcp.capabilities.prompt.PromptMessage as DomainPromptMessage
import org.omniai.mcp.capabilities.prompt.PromptRole
import org.omniai.mcp.capabilities.resource.McpResource
import org.omniai.mcp.capabilities.tool.McpTool
import org.omniai.mcp.capabilities.tool.ToolContent
import org.omniai.mcp.capabilities.tool.ToolResult

internal object DomainMapper {

    fun mapTool(domainTool: McpTool<*>): Tool {
        return Tool(
            name = domainTool.name,
            description = domainTool.description,
            inputSchema = ToolSchema(
                properties = domainTool.schema.properties,
                required = domainTool.schema.required
            )
        )
    }

    fun mapResource(domainResource: McpResource): Resource {
        return Resource(
            uri = domainResource.uri,
            name = domainResource.name,
            description = domainResource.description,
            mimeType = domainResource.mimeType
        )
    }

    fun mapPrompt(domainPrompt: McpPrompt): Prompt {
        return Prompt(
            name = domainPrompt.name,
            description = domainPrompt.description,
            arguments = emptyList()
        )
    }

    fun mapPromptMessageRole(role: PromptRole): Role {
        return when (role) {
            PromptRole.USER -> Role.User
            PromptRole.ASSISTANT -> Role.Assistant
        }
    }

    fun mapPromptMessage(domainMessage: DomainPromptMessage): PromptMessage {
        return PromptMessage(
            role = mapPromptMessageRole(domainMessage.role),
            content = mapToolContent(domainMessage.content)
        )
    }

    fun mapToolContent(content: ToolContent): ContentBlock {
        return when (content) {
            is ToolContent.Text -> TextContent(text = content.text)
            is ToolContent.Image -> ImageContent(data = content.data, mimeType = content.mimeType)
            is ToolContent.Resource -> {
                val resourceContents = if (content.resource.text != null) {
                    TextResourceContents(
                        uri = content.resource.uri,
                        mimeType = content.resource.mimeType,
                        text = content.resource.text
                    )
                } else {
                    BlobResourceContents(
                        uri = content.resource.uri,
                        mimeType = content.resource.mimeType,
                        blob = content.resource.blob ?: ""
                    )
                }

                EmbeddedResource(resource = resourceContents)
            }
        }
    }

    fun mapCallToolResult(domainResult: ToolResult): CallToolResult {
        return CallToolResult(
            content = domainResult.content.map { mapToolContent(it) },
            isError = domainResult.isError
        )
    }
}