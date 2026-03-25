package org.omniai.sdk.contracts.gemini.input

import kotlinx.serialization.Serializable

@Serializable
data class GeminiSystemInstruction(
    val parts: List<GeminiPart>
)

