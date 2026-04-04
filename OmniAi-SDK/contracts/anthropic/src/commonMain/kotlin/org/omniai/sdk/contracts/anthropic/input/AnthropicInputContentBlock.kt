package org.omniai.sdk.contracts.anthropic.input

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonObject

@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
sealed interface AnthropicInputContentBlock {

    @Serializable
    @SerialName("text")
    data class Text(
        val text: String
    ) : AnthropicInputContentBlock

    @Serializable
    @SerialName("tool_use")
    data class ToolUse(
        val id: String? = null,
        val name: String,
        val input: JsonObject? = null
    ) : AnthropicInputContentBlock

    @Serializable
    @SerialName("tool_result")
    data class ToolResult(
        @SerialName("tool_use_id")
        val toolUseId: String,
        val content: String
    ) : AnthropicInputContentBlock

    @Serializable
    @SerialName("thinking")
    data class Thinking(
        @SerialName("budget_tokens")
        val budgetTokens: Int? = null
    ) : AnthropicInputContentBlock

}
