package org.omniaigateway.contracts.anthropic.output

sealed interface AnthropicOutputContent {
    val type: String

    data class Text(
        val text: String,
        override val type: String = "text"
    ) : AnthropicOutputContent

    data class Thinking(
        val thinking: String,
        val signature: String? = null,
        override val type: String = "thinking"
    ) : AnthropicOutputContent

    data class ToolUse(
        val id: String,
        val name: String,
        val input: Map<String, Any?>? = null,
        override val type: String = "tool_use"
    ) : AnthropicOutputContent
}
