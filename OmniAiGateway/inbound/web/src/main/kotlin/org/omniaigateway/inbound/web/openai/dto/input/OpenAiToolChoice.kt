package org.omniaigateway.inbound.web.openai.dto.input

sealed interface OpenAiToolChoice {
    data class Mode(val value: String) : OpenAiToolChoice // "auto" | "none" | "required"
    data class Function(val type: String = "function", val function: FunctionRef) : OpenAiToolChoice
}

data class FunctionRef(
    val name: String,
)