package org.omniai.sdk.contracts.anthropic.output

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.omniai.sdk.contracts.anthropic.serialization.AnthropicOutputContentSerializer

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
        val input: JsonObject? = null,
        override val type: String = "tool_use"
    ) : AnthropicOutputContent
}
