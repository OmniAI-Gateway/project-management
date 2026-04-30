package org.omniai.sdk.auth.domain

data class IntrospectionResult(
    val active: Boolean,
    val sub: String? = null,
    val scope: String? = null,
    val clientId: String? = null,
    val username: String? = null,
    val exp: Long? = null,
    val iat: Long? = null,
    val iss: String? = null,
    val aud: List<String> = emptyList()
)
