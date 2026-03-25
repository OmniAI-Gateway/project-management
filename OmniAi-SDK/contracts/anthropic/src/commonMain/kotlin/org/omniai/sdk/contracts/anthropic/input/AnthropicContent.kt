package org.omniai.sdk.contracts.anthropic.input

import kotlinx.serialization.Serializable
import org.omniai.sdk.contracts.anthropic.serialization.AnthropicContentSerializer

@Serializable(with = AnthropicContentSerializer::class)
sealed interface AnthropicContent
