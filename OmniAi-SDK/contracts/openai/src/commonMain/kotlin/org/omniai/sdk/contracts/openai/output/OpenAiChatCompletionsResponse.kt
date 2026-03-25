package org.omniai.sdk.contracts.openai.output

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiChatCompletionsResponse(
    val id: String,
    @SerialName("object")
    val obj: String,
    val created: Long,
    val model: String,
    @SerialName("system_fingerprint")
    val systemFingerprint: String? = null,
    val choices: List<OpenAiChoice>,
    val usage: OpenAiUsage? = null,
)
