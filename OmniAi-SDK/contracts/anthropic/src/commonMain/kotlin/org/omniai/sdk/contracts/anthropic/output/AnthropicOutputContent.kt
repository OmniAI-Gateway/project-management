package org.omniai.sdk.contracts.anthropic.output

import kotlinx.serialization.Serializable
import org.omniai.sdk.contracts.anthropic.serialization.AnthropicOutputContentSerializer
import org.omniai.sdk.contracts.anthropic.serialization.NullableStringAnyMapSerializer

@Serializable(with = AnthropicOutputContentSerializer::class)
sealed interface AnthropicOutputContent {
    val type: String

    @Serializable
    data class Text(
        val text: String,
        override val type: String = "text"
    ) : AnthropicOutputContent

    @Serializable
    data class Thinking(
        val thinking: String,
        val signature: String? = null,
        override val type: String = "thinking"
    ) : AnthropicOutputContent

    @Serializable
    data class ToolUse(
        val id: String,
        val name: String,
        @Serializable(with = NullableStringAnyMapSerializer::class)
        val input: Map<String, Any?>? = null,
        override val type: String = "tool_use"
    ) : AnthropicOutputContent
}
