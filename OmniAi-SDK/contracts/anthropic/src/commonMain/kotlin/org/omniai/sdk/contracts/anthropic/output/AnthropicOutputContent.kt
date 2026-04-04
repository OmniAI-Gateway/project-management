package org.omniai.sdk.contracts.anthropic.output

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonObject

@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
sealed interface AnthropicOutputContent {

    @Serializable
    @SerialName("text")
    data class Text(
        val text: String
    ) : AnthropicOutputContent

    @Serializable
    @SerialName("thinking")
    data class Thinking(
        val thinking: String,
        val signature: String? = null
    ) : AnthropicOutputContent

    @Serializable
    @SerialName("tool_use")
    data class ToolUse(
        val id: String,
        val name: String,
        val input: JsonObject? = null
    ) : AnthropicOutputContent

}
