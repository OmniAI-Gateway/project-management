package org.omniaigateway.contracts.openai.input

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiTool(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val type: String = "function",
    val function: OpenAiFunctionDefinition,
)
