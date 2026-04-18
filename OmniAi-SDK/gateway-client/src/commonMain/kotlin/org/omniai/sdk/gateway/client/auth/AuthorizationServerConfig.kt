package org.omniai.sdk.gateway.client.auth

import org.omniai.sdk.interceptors.auth.domain.TokenAuthenticator

sealed interface AuthorizationServerConfig {
    data object None : AuthorizationServerConfig

    data class Discovery(
        val discoveryUrl: String,
        val expectedAudience: String,
        val clientId: String? = null,
        val clientSecret: String? = null
    ) : AuthorizationServerConfig

    data class Custom(
        val authenticator: TokenAuthenticator
    ) : AuthorizationServerConfig
}

class AuthorizationServerDsl {
    private var config: AuthorizationServerConfig = AuthorizationServerConfig.None

    fun none() {
        config = AuthorizationServerConfig.None
    }

    fun discovery(block: DiscoveryAuthorizationServerDsl.() -> Unit) {
        config = DiscoveryAuthorizationServerDsl().apply(block).build()
    }

    fun custom(authenticator: TokenAuthenticator) {
        config = AuthorizationServerConfig.Custom(authenticator)
    }

    internal fun build(): AuthorizationServerConfig = config
}

class DiscoveryAuthorizationServerDsl {
    var discoveryUrl: String = ""
    var expectedAudience: String = ""
    var clientId: String? = null
    var clientSecret: String? = null

    internal fun build(): AuthorizationServerConfig.Discovery {
        require(discoveryUrl.isNotBlank()) { "Authorization discoveryUrl cannot be blank." }
        require(expectedAudience.isNotBlank()) { "Authorization expectedAudience cannot be blank." }

        return AuthorizationServerConfig.Discovery(
            discoveryUrl = discoveryUrl,
            expectedAudience = expectedAudience,
            clientId = clientId,
            clientSecret = clientSecret
        )
    }
}

