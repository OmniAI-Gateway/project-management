package org.omniaigateway.contracts.openai.input

sealed interface OpenAiToolChoice {
    data class Mode(val value: String) : OpenAiToolChoice // "auto" | "none" | "required"
    data class Function(val type: String = "function", val function: FunctionRef) : OpenAiToolChoice
}

data class FunctionRef(
    val name: String,
)