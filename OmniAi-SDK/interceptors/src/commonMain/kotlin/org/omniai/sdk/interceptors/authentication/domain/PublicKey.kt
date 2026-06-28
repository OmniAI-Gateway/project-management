package org.omniai.sdk.interceptors.auth.domain

data class PublicKey(
    val key: Base64,
    val algorithm: String,
)
