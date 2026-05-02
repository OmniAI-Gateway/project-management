package org.omniai.mcp.capabilities.prompt

class McpPrompt(
    val name: String,
    val description: String? = null,
    val handler: suspend (args: Map<String, String>?) -> List<PromptMessage>
)
