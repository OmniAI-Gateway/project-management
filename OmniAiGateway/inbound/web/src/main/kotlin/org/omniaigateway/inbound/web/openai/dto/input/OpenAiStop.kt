package org.omniaigateway.inbound.web.openai.dto.input

sealed interface OpenAiStop {
    data class Single(val value: String) : OpenAiStop
    data class Multiple(val values: List<String>) : OpenAiStop
}