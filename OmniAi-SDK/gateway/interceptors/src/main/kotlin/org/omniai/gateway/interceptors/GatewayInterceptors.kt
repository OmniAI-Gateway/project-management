package org.omniai.gateway.interceptors

import org.omniai.gateway.interceptors.auth.OpaqueAwareTokenAuthenticator
import org.omniai.gateway.interceptors.auth.StubOpaqueTokenIntrospector
import org.omniai.sdk.core.pipeline.AuthContextInterceptor
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.JoseJwtTokenAuthenticator
import org.omniai.sdk.core.pipeline.JwtAuthConfig
import org.omniai.sdk.core.pipeline.MetricsInterceptor
import org.omniai.sdk.core.pipeline.PassThroughTokenAuthenticator
import org.omniai.sdk.core.pipeline.TokenAuthenticator

enum class AuthMode {
    OFF,
    JWT_ONLY,
    JWT_OPAQUE_PREPARED
}

fun defaultGatewayInterceptors(): List<Interceptor> = listOf(
    AuthContextInterceptor(authenticator = loadTokenAuthenticator()),
    RequestLoggingInterceptor(),
    MetricsInterceptor()
)

private fun loadTokenAuthenticator(): TokenAuthenticator = when (loadAuthMode()) {
    AuthMode.OFF -> PassThroughTokenAuthenticator()
    AuthMode.JWT_ONLY -> loadJwtAuthenticator()
    AuthMode.JWT_OPAQUE_PREPARED -> OpaqueAwareTokenAuthenticator(
        jwtAuthenticator = loadJwtAuthenticator(),
        opaqueTokenIntrospector = StubOpaqueTokenIntrospector(),
        opaqueIntrospectionEnabled = System.getenv("AUTH_OPAQUE_INTROSPECTION_ENABLED")
            ?.equals("true", ignoreCase = true)
            ?: false
    )
}

private fun loadAuthMode(): AuthMode {
    val raw = System.getenv("AUTH_MODE")?.trim()?.uppercase() ?: AuthMode.OFF.name
    return runCatching { AuthMode.valueOf(raw) }.getOrElse { AuthMode.OFF }
}

private fun loadJwtAuthenticator(): TokenAuthenticator {
    val issuer = requireEnv("AUTH_JWT_ISSUER")
    val audience = requireEnv("AUTH_JWT_AUDIENCE")
    val jwksUrl = requireEnv("AUTH_JWKS_URL")

    val clockSkew = System.getenv("AUTH_JWT_CLOCK_SKEW_SECONDS")?.toLongOrNull() ?: 60L
    val connectTimeout = System.getenv("AUTH_JWKS_CONNECT_TIMEOUT_MS")?.toIntOrNull() ?: 2_000
    val readTimeout = System.getenv("AUTH_JWKS_READ_TIMEOUT_MS")?.toIntOrNull() ?: 2_000
    val allowedAlgorithm = System.getenv("AUTH_JWT_ALLOWED_ALG") ?: "RS256"

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

private fun requireEnv(name: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: throw IllegalStateException("Environment variable '$name' is required for selected AUTH_MODE")

