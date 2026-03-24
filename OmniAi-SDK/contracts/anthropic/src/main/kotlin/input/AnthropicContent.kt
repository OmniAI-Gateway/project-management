package org.omniaigateway.contracts.anthropic.input

import kotlinx.serialization.Serializable
import org.omniaigateway.contracts.anthropic.serialization.AnthropicContentSerializer

@Serializable(with = AnthropicContentSerializer::class)
sealed interface AnthropicContent
