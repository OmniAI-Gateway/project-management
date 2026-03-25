package org.omniai.sdk.contracts.anthropic.input

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement


@Serializable
data class AnthropicOutputFormat(
    val type: String,
    val schema: JsonElement? = null
)
