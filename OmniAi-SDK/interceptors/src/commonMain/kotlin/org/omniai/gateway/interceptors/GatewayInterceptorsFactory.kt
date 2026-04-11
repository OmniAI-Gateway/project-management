package org.omniai.gateway.interceptors

import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.MetricsInterceptor
import org.omniai.sdk.interceptors.auth.AuthContextInterceptor
import org.omniai.sdk.interceptors.auth.ConfigSource
import org.omniai.sdk.interceptors.auth.JoseJwtTokenAuthenticator
import org.omniai.sdk.interceptors.auth.JwtAuthConfig
import org.omniai.sdk.interceptors.auth.OpaqueAwareTokenAuthenticator
import org.omniai.sdk.interceptors.auth.PassThroughTokenAuthenticator
import org.omniai.sdk.interceptors.auth.StubOpaqueTokenIntrospector
import org.omniai.sdk.interceptors.auth.TokenAuthenticator

enum class AuthMode {
    OFF,
    JWT_ONLY,
    JWT_OPAQUE_PREPARED
}

fun defaultGatewayInterceptors(
    configSource: ConfigSource,
    logger: GatewayLogger = NoOpGatewayLogger
): List<Interceptor> = listOf(
    AuthContextInterceptor(authenticator = loadTokenAuthenticator(configSource)),
    RequestLoggingInterceptor(logger),
    MetricsInterceptor()
)

private fun loadTokenAuthenticator(configSource: ConfigSource): TokenAuthenticator = when (loadAuthMode(configSource)) {
    AuthMode.OFF -> PassThroughTokenAuthenticator()
    AuthMode.JWT_ONLY -> loadJwtAuthenticator(configSource)
    AuthMode.JWT_OPAQUE_PREPARED -> OpaqueAwareTokenAuthenticator(
        jwtAuthenticator = loadJwtAuthenticator(configSource),
        opaqueTokenIntrospector = StubOpaqueTokenIntrospector(),
        opaqueIntrospectionEnabled = configSource.get("AUTH_OPAQUE_INTROSPECTION_ENABLED")
            ?.equals("true", ignoreCase = true)
            ?: false
    )
}

private fun loadAuthMode(configSource: ConfigSource): AuthMode {
    val raw = configSource.get("AUTH_MODE")?.trim()?.uppercase() ?: AuthMode.OFF.name
    return runCatching { AuthMode.valueOf(raw) }.getOrElse { AuthMode.OFF }
}

private fun loadJwtAuthenticator(configSource: ConfigSource): TokenAuthenticator {
    val issuer = requireConfig(configSource, "AUTH_JWT_ISSUER")
    val audience = requireConfig(configSource, "AUTH_JWT_AUDIENCE")
    val jwksUrl = requireConfig(configSource, "AUTH_JWKS_URL")

    val clockSkew = configSource.get("AUTH_JWT_CLOCK_SKEW_SECONDS")?.toLongOrNull() ?: 60L
    val connectTimeout = configSource.get("AUTH_JWKS_CONNECT_TIMEOUT_MS")?.toIntOrNull() ?: 2_000
    val readTimeout = configSource.get("AUTH_JWKS_READ_TIMEOUT_MS")?.toIntOrNull() ?: 2_000
    val allowedAlgorithm = configSource.get("AUTH_JWT_ALLOWED_ALG") ?: "RS256"

    return JoseJwtTokenAuthenticator(
        config = JwtAuthConfig(
            issuer = issuer,
            audience = audience,
            jwksUrl = jwksUrl,
            allowedAlgorithm = allowedAlgorithm,
            clockSkewSeconds = clockSkew,
            connectTimeoutMillis = connectTimeout,
            readTimeoutMillis = readTimeout
        )
    )
}

private fun requireConfig(configSource: ConfigSource, name: String): String =
    configSource.get(name)?.takeIf { it.isNotBlank() }
        ?: throw IllegalStateException("Configuration '$name' is required for selected AUTH_MODE")

