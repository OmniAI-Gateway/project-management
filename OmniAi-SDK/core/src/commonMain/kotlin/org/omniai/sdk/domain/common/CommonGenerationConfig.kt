package org.omniai.sdk.domain.common

data class CommonGenerationConfig(
    val temperature: Double? = 0.7,
    val maxTokens: Int? = null,
    val topP: Double? = null,
    val stopSequences: List<String>? = null
)

