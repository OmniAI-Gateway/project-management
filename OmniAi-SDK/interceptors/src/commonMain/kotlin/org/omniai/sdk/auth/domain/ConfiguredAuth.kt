package org.omniai.sdk.auth.domain

import org.omniai.sdk.auth.interfaces.TokenAuthenticator

data class ConfiguredAuth(
    val authenticator: TokenAuthenticator,
    val validationParams: TokenValidationParams
)