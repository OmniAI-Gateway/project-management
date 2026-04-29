package org.omniai.sdk.auth.domain

fun detectTokenKind(token: String): TokenKind {
    val segments = token.split('.')
    return if (segments.size == 3 && segments.none { it.isBlank() }) {
        TokenKind.JWT
    } else {
        TokenKind.OPAQUE
    }
}

