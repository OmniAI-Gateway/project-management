package org.omniai.sdk.interceptors.auth.config

import org.omniai.sdk.interceptors.auth.cache.InMemoryIntrospectionCache
import org.omniai.sdk.interceptors.auth.cache.InMemoryKeyCache
import org.omniai.sdk.interceptors.auth.domain.AuthSetupConfig
import org.omniai.sdk.interceptors.auth.domain.ConfiguredAuth
import org.omniai.sdk.interceptors.auth.domain.HttpAuthSecurityClientConfig
import org.omniai.sdk.interceptors.auth.domain.TokenValidationParams
import org.omniai.sdk.interceptors.auth.interfaces.IntrospectionCache
import org.omniai.sdk.interceptors.auth.interfaces.PublicKeyCache
import org.omniai.sdk.interceptors.auth.oidc.OidcDiscovery
import org.omniai.sdk.ports.outbound.http.HttpCallResult
import org.omniai.sdk.ports.outbound.http.toDomainError
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.interceptors.auth.authenticators.*
import org.omniai.sdk.interceptors.auth.engines.*
import org.omniai.sdk.interceptors.auth.client.*
import kotlin.time.Duration

internal suspend fun loadTokenAuthenticator(
    config: AuthSetupConfig,
    publicKeyCache: PublicKeyCache = InMemoryKeyCache(),
    introspectionCache: IntrospectionCache? = null,
    positiveCacheTtl: Duration? = null,
    negativeCacheTtl: Duration? = null,
): ConfiguredAuth {
    return when (config) {
        is AuthSetupConfig.Off -> {
            ConfiguredAuth(authenticator = PassThroughTokenAuthenticator())
        }

        is AuthSetupConfig.Discovery -> {
            val discovery = OidcDiscovery(config.httpClient)

            val metadata = when (val result = discovery.fetchMetadata(config.discoveryUrl)) {
                is HttpCallResult.Success -> result.data
                is HttpCallResult.ApiError -> throw RuntimeException(
                    "Falha no Discovery: ${result.toDomainError(Provider("auth-server")).message}"
                )
                else -> throw RuntimeException("Erro inesperado no Discovery de OIDC")
            }

            val infraConfig = HttpAuthSecurityClientConfig(
                authClientId = config.authClientId,
                authClientSecret = config.authClientSecret
            )

            val resolvedCache = introspectionCache ?: InMemoryIntrospectionCache(
                positiveCacheTtl = positiveCacheTtl ?: infraConfig.introspectionCacheTtl,
                negativeCacheTtl = negativeCacheTtl ?: infraConfig.introspectionNegativeCacheTtl,
            )

            val jwksClient = JwksClient(
                config = infraConfig,
                publicKeyCache = publicKeyCache,
                httpClient = config.httpClient,
                jwksUri = metadata.jwksUri,
            )

            val introspectionClient = metadata.introspectionEndpoint?.let { endpoint ->
                HttpIntrospectionClient(
                    cache = resolvedCache,
                    endpoint = endpoint,
                    clientId = config.authClientId,
                    clientSecret = config.authClientSecret,
                    httpClient = config.httpClient,
                )
            }

            val authInfra = HttpAuthSecurityClient(
                jwksClient = jwksClient,
                introspectionClient = introspectionClient,
            )

            val authenticator = DefaultTokenAuthenticator(
                infra = authInfra,
                jwtVerificationEngine = PlatformJwtVerificationEngine(),
            )

            val params = TokenValidationParams(
                expectedIssuer = metadata.issuer,
                expectedAudience = config.expectedAudience,
            )

            ConfiguredAuth(authenticator, params)
        }
    }
}
