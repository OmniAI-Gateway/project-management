package org.omniai.sdk.auth.dto

import kotlinx.serialization.Serializable


@Serializable
data class JwksDto(
    val keys: List<JwkDto>
)
