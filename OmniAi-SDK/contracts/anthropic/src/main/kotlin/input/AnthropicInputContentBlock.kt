package org.omniaigateway.contracts.anthropic.input

sealed interface AnthropicInputContentBlock {
    val type: String

    data class Text(
        val text: String,
        override val type: String = "text"
    ) : AnthropicInputContentBlock

    data class ToolUse(
        val id: String? = null,
        val name: String,
        val input: Map<String, Any?>? = null,
        override val type: String = "tool_use"
    ) : AnthropicInputContentBlock

    data class ToolResult(
        val toolUseId: String,
        val content: String,
        override val type: String = "tool_result"
    ) : AnthropicInputContentBlock

    data class Thinking(
        val budgetTokens: Int? = null,
        override val type: String = "thinking"
    ) : AnthropicInputContentBlock
}
