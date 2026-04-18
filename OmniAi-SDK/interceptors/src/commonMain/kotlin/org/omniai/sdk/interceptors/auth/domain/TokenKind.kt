package org.omniai.sdk.interceptors.auth.domain

enum class TokenKind { JWT, OPAQUE }

fun detectTokenKind(token: String): TokenKind {
    val segments = token.split('.')
    return if (segments.size == 3 && segments.none { it.isBlank() }) {
        TokenKind.JWT
    } else {
        TokenKind.OPAQUE
    }
}