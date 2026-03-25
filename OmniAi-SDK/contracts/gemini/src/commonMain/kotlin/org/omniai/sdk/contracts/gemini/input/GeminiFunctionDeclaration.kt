package org.omniai.sdk.contracts.gemini.input

import kotlinx.serialization.Serializable
import org.omniai.sdk.contracts.gemini.serialization.StringAnyMapSerializer

@Serializable
data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    @Serializable(with = StringAnyMapSerializer::class)
    val parameters: Map<String, Any?>
)
