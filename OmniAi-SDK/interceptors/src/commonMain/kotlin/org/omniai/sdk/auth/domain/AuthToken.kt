package org.omniai.sdk.auth.domain

data class AuthToken(
    val rawValue: String,
    val kind: TokenKind
)