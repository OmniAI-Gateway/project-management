package org.omniai.sdk.auth.domain

data class TokenValidationParams(
    val expectedIssuer: String,
    val expectedAudience: String
)