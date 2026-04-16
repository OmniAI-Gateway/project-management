package org.omniai.gateway.interceptors

import org.omniai.sdk.core.http.HttpCallResult
import org.omniai.sdk.core.http.HttpTransportClient
import org.omniai.sdk.core.http.toDomainError
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.interceptors.auth.ConfigSource
import org.omniai.sdk.interceptors.auth.HttpAuthSecurityClient
import org.omniai.sdk.interceptors.auth.JoseJwtTokenAuthenticator
import org.omniai.sdk.interceptors.auth.OidcDiscovery
import org.omniai.sdk.interceptors.auth.PassThroughTokenAuthenticator
import org.omniai.sdk.interceptors.auth.TokenAuthenticator
import org.omniai.sdk.interceptors.auth.cache.CachedAuthInfrastructure
import org.omniai.sdk.interceptors.auth.cache.CachedPublicKeysProvider

enum class AuthMode {
    OFF,
    DISCOVERY
}

internal suspend fun loadTokenAuthenticator(
    configSource: ConfigSource,
    httpClient: HttpTransportClient
): TokenAuthenticator =
    when (loadAuthMode(configSource)) {
        AuthMode.OFF -> PassThroughTokenAuthenticator()
        AuthMode.DISCOVERY -> loadDiscoveryAuthenticator(configSource, httpClient)
    }


private suspend fun loadDiscoveryAuthenticator(
    configSource: ConfigSource,
    httpClient: HttpTransportClient
): TokenAuthenticator {
    val discoveryUrl = requireConfig(configSource, "AUTH_DISCOVERY_URL")
    val discovery = OidcDiscovery(httpClient)
    val metadata = when (val result = discovery.fetchMetadata(discoveryUrl)) {
        is HttpCallResult.Success -> result.data
        is HttpCallResult.ApiError -> throw RuntimeException("Falha no Discovery: ${result.toDomainError(Provider("auth-server")).message}")
        else -> throw RuntimeException("Erro inesperado no Discovery")
    }

    val authInfra = HttpAuthSecurityClient(
        httpClient = httpClient,
        jwksUri = metadata.jwksUri,
        tokenEndpoint = metadata.tokenEndpoint,
        configSource = configSource
    )

    val cache = CachedPublicKeysProvider(delegate = authInfra)
    val infraCompleted= CachedAuthInfrastructure(authInfra, cache)

    val expectedAudience = requireConfig(configSource, "AUTH_JWT_AUDIENCE")
    return JoseJwtTokenAuthenticator(
        infra = infraCompleted,
        expectedIssuer = metadata.issuer,
        expectedAudience = expectedAudience
    )
}

private fun loadAuthMode(configSource: ConfigSource): AuthMode =
    runCatching { AuthMode.valueOf(configSource.get("AUTH_MODE")?.trim()?.uppercase() ?: AuthMode.OFF.name) }.getOrElse { AuthMode.OFF }

private fun requireConfig(configSource: ConfigSource, name: String): String =
    configSource.get(name) ?: throw IllegalStateException("Configuração obrigatória em falta: $name")