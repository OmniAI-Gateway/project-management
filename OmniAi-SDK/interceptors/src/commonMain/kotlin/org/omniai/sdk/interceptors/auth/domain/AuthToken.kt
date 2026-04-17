package org.omniai.sdk.interceptors.auth.domain

data class AuthToken(
    val rawValue: String,
    val kind: TokenKind
)