package org.omniai.sdk.gateway.client.auth


import org.omniai.sdk.interceptors.auth.interfaces.TokenAuthenticator
import org.omniai.sdk.interceptors.auth.interfaces.IntrospectionCache
import kotlin.time.Duration

sealed interface AuthorizationServerConfig {

    object None : AuthorizationServerConfig

    data class Custom(
        val authenticator: TokenAuthenticator
    ) : AuthorizationServerConfig

    data class Discovery(
        val discoveryUrl: String,
        val expectedAudience: String,
        val clientId: String? = null,
        val clientSecret: String? = null,
        val introspectionCache: IntrospectionCache? = null,
        val positiveCacheTtl: Duration? = null,
        val negativeCacheTtl: Duration? = null
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
    var introspectionCache: IntrospectionCache? = null
    var positiveCacheTtl: Duration? = null
    var negativeCacheTtl: Duration? = null

    internal fun build(): AuthorizationServerConfig.Discovery {
        require(discoveryUrl.isNotBlank()) { "Authorization discoveryUrl cannot be blank." }
        require(expectedAudience.isNotBlank()) { "Authorization expectedAudience cannot be blank." }

        return AuthorizationServerConfig.Discovery(
            discoveryUrl = discoveryUrl,
            expectedAudience = expectedAudience,
            clientId = clientId,
            clientSecret = clientSecret,
            introspectionCache = introspectionCache,
            positiveCacheTtl = positiveCacheTtl,
            negativeCacheTtl = negativeCacheTtl
        )
    }
}
