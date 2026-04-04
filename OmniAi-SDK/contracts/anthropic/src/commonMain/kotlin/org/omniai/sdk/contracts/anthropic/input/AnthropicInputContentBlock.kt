package org.omniai.sdk.contracts.anthropic.input

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.omniai.sdk.contracts.anthropic.serialization.AnthropicInputContentBlockSerializer

@Serializable(with = AnthropicInputContentBlockSerializer::class)
sealed interface AnthropicInputContentBlock {
    val type: String

    @Serializable
    data class Text(
        val text: String,
        override val type: String = "text"
    ) : AnthropicInputContentBlock

    @Serializable
    data class ToolUse(
        val id: String? = null,
        val name: String,
        val input: JsonObject? = null,
        override val type: String = "tool_use"
    ) : AnthropicInputContentBlock

    @Serializable
    data class ToolResult(
        @SerialName("tool_use_id")
        val toolUseId: String,
        val content: String,
        override val type: String = "tool_result"
    ) : AnthropicInputContentBlock

    @Serializable
    data class Thinking(
        @SerialName("budget_tokens")
        val budgetTokens: Int? = null,
        override val type: String = "thinking"
    ) : AnthropicInputContentBlock
}
