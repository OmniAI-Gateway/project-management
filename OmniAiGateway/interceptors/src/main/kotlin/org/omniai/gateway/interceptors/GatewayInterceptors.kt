package org.omniai.gateway.interceptors

import RequestLoggingInterceptor
import org.omniai.gateway.interceptors.auth.JoseJwtTokenAuthenticator
import org.omniai.gateway.interceptors.auth.JwtAuthConfig
import org.omniai.gateway.interceptors.auth.OpaqueAwareTokenAuthenticator
import org.omniai.gateway.interceptors.auth.StubOpaqueTokenIntrospector
import org.omniai.sdk.core.pipeline.AuthContextInterceptor
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.MetricsInterceptor
import org.omniai.sdk.core.pipeline.PassThroughTokenAuthenticator
import org.omniai.sdk.core.pipeline.TokenAuthenticator

enum class AuthMode {
    OFF,
    JWT_ONLY,
    JWT_OPAQUE_PREPARED
}

// Monta a cadeia padrão de interceptors usada pela pipeline da gateway.
fun defaultGatewayInterceptors(): List<Interceptor> = listOf(
    AuthContextInterceptor(authenticator = loadTokenAuthenticator()),
    RequestLoggingInterceptor(),
    MetricsInterceptor()
)

// Escolhe a estratégia de autenticação de token com base no AUTH_MODE.
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

// Lê e interpreta o AUTH_MODE do ambiente; se for inválido, usa OFF.
private fun loadAuthMode(): AuthMode {
    val raw = System.getenv("AUTH_MODE")?.trim()?.uppercase() ?: AuthMode.OFF.name
    return runCatching { AuthMode.valueOf(raw) }.getOrElse { AuthMode.OFF }
}

// Cria o autenticador JWT (JOSE) a partir das variáveis de ambiente.
private fun loadJwtAuthenticator(): TokenAuthenticator {
    val issuer = requireEnv("AUTH_JWT_ISSUER")
    val audience = requireEnv("AUTH_JWT_AUDIENCE")
    val jwksUrl = requireEnv("AUTH_JWKS_URL")

    val clockSkew = System.getenv("AUTH_JWT_CLOCK_SKEW_SECONDS")?.toLongOrNull() ?: 60L
    val connectTimeout = System.getenv("AUTH_JWKS_CONNECT_TIMEOUT_MS")?.toIntOrNull() ?: 2_000
    val readTimeout = System.getenv("AUTH_JWKS_READ_TIMEOUT_MS")?.toIntOrNull() ?: 2_000

    return JoseJwtTokenAuthenticator(
        config = JwtAuthConfig(
            issuer = issuer,
            audience = audience,
            jwksUrl = jwksUrl,
            clockSkewSeconds = clockSkew,
            connectTimeoutMillis = connectTimeout,
            readTimeoutMillis = readTimeout
        )
    )
}

// Lê uma env var obrigatória para o modo de autenticação selecionado.
private fun requireEnv(name: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: throw IllegalStateException("Environment variable '$name' is required for selected AUTH_MODE")
