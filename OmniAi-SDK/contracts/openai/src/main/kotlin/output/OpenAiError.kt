package org.omniaigateway.contracts.openai.output

data class OpenAiError(
    val message: String,
    val type: String? = null,
    val param: String? = null,
    val code: String? = null,
)
