package org.omniai.sdk.interceptors.auth.config

import org.omniai.sdk.interceptors.auth.cache.InMemoryKeyCache
import org.omniai.sdk.interceptors.auth.domain.AuthSetupConfig
import org.omniai.sdk.interceptors.auth.domain.ConfiguredAuth

import org.omniai.sdk.interceptors.auth.domain.HttpAuthSecurityClientConfig
import org.omniai.sdk.interceptors.auth.domain.TokenValidationParams
import org.omniai.sdk.interceptors.auth.interfaces.PublicKeyCache
import org.omniai.sdk.interceptors.auth.oidc.OidcDiscovery
import org.omniai.sdk.core.http.HttpCallResult
import org.omniai.sdk.core.http.toDomainError
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.interceptors.auth.authenticators.*
import org.omniai.sdk.interceptors.auth.engines.*
import org.omniai.sdk.interceptors.auth.client.*

internal suspend fun loadTokenAuthenticator(
    config: AuthSetupConfig,
    cache: PublicKeyCache = InMemoryKeyCache()
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

            val authInfra = HttpAuthSecurityClient(
                config = infraConfig,
                publicKeyCache = cache,
                httpClient = config.httpClient,
                jwksUri = metadata.jwksUri,
                introspectionEndpoint = metadata.introspectionEndpoint
            )
            val authenticator = DefaultTokenAuthenticator(
                infra = authInfra,
                jwtVerificationEngine = PlatformJwtVerificationEngine()
            )
            val params = TokenValidationParams(
                expectedIssuer = metadata.issuer,
                expectedAudience = config.expectedAudience
            )
            ConfiguredAuth(authenticator, params)
        }
    }
}

