package org.omniai.mcp.capabilities.prompt

import org.omniai.mcp.capabilities.tool.ToolContent

enum class PromptRole { USER, ASSISTANT }

data class PromptMessage(
    val role: PromptRole,
    val content: ToolContent 
)
