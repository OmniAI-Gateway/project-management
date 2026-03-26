package org.omniai.sdk.contracts.anthropic.input

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnthropicThinkingConfig(
    val type: String,
    @SerialName("budget_tokens")
    val budgetTokens: Int = 1024,
)
