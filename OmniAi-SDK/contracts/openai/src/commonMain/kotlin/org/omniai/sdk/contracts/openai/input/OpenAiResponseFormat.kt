package org.omniai.sdk.contracts.openai.input

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiResponseFormat(
    val type: String,
    @SerialName("json_schema")
    val jsonSchema: OpenAiJsonSchema? = null
)
