package org.omniai.sdk.interceptors.auth.domain

import org.omniai.sdk.interceptors.auth.interfaces.TokenAuthenticator

data class ConfiguredAuth(
    val authenticator: TokenAuthenticator,
    val validationParams: TokenValidationParams? = null
)