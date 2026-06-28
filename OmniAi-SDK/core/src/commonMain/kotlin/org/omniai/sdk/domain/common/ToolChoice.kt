package org.omniai.sdk.domain.common

sealed class ToolChoice {
    data object Auto : ToolChoice()

    data object None : ToolChoice()

    data object Required : ToolChoice()

    data class Specific(
        val toolNames: List<String>,
    ) : ToolChoice() {
        constructor(name: String) : this(listOf(name))
    }
}
