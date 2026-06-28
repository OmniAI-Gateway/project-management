package org.omniai.sdk.interceptors.auth.domain

data class TokenValidationParams(
    val expectedIssuer: String,
    val expectedAudience: String,
)
