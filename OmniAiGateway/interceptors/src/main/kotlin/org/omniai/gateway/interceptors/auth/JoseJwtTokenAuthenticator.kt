package org.omniai.gateway.interceptors.auth

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.JWKMatcher
import com.nimbusds.jose.jwk.JWKSelector
import com.nimbusds.jose.jwk.KeyType
import com.nimbusds.jose.jwk.source.RemoteJWKSet
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jose.util.DefaultResourceRetriever
import com.nimbusds.jwt.SignedJWT
import java.net.URL
import java.time.Instant
import org.omniai.sdk.core.pipeline.AuthToken
import org.omniai.sdk.core.pipeline.AuthenticationDecision
import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.core.pipeline.TokenAuthenticator
import org.omniai.sdk.core.pipeline.TokenKind

data class JwtAuthConfig(
    val issuer: String,
    val audience: String,
    val jwksUrl: String,
    val allowedAlgorithm: JWSAlgorithm = JWSAlgorithm.RS256,
    val clockSkewSeconds: Long = 60,
    val connectTimeoutMillis: Int = 2_000,
    val readTimeoutMillis: Int = 2_000
)

class JoseJwtTokenAuthenticator(
    private val config: JwtAuthConfig,
    private val nowProvider: () -> Instant = { Instant.now() }
) : TokenAuthenticator {

    private val jwkSource = RemoteJWKSet<SecurityContext>(
        URL(config.jwksUrl),
        DefaultResourceRetriever(config.connectTimeoutMillis, config.readTimeoutMillis)
    )

    // Valida JWT (formato, algoritmo, assinatura e claims padrão); opaque não é tratado aqui.
    override suspend fun authenticate(token: AuthToken, context: GatewayContext): AuthenticationDecision {
        if (token.kind == TokenKind.OPAQUE) {
            return AuthenticationDecision.Allow
        }

        val signedJwt = runCatching { SignedJWT.parse(token.rawValue) }
            .getOrElse { return AuthenticationDecision.Deny("Malformed JWT") }

        val header = signedJwt.header
        if (header.algorithm != config.allowedAlgorithm) {
            return AuthenticationDecision.Deny("Unsupported JWT algorithm")
        }

        if (!verifySignature(signedJwt, header.keyID)) {
            return AuthenticationDecision.Deny("Invalid JWT signature")
        }

        val claims = signedJwt.jwtClaimsSet
        val now = nowProvider()

        if (claims.issuer != config.issuer) {
            return AuthenticationDecision.Deny("Invalid JWT issuer")
        }

        if (!claims.audience.contains(config.audience)) {
            return AuthenticationDecision.Deny("Invalid JWT audience")
        }

        claims.expirationTime?.toInstant()?.let { exp ->
            if (now.isAfter(exp.plusSeconds(config.clockSkewSeconds))) {
                return AuthenticationDecision.Deny("JWT expired")
            }
        } ?: return AuthenticationDecision.Deny("Missing JWT exp claim")

        claims.notBeforeTime?.toInstant()?.let { nbf ->
            if (now.plusSeconds(config.clockSkewSeconds).isBefore(nbf)) {
                return AuthenticationDecision.Deny("JWT not active yet")
            }
        }

        return AuthenticationDecision.Allow
    }

    // Procura chaves no JWKS e tenta validar a assinatura com alguma chave RSA compatível.
    private fun verifySignature(jwt: SignedJWT, keyId: String?): Boolean {
        val keys = runCatching {
            val matcherBuilder = JWKMatcher.Builder()
                .keyType(KeyType.RSA)
                .algorithm(config.allowedAlgorithm)
            if (!keyId.isNullOrBlank()) {
                matcherBuilder.keyID(keyId)
            }
            jwkSource.get(JWKSelector(matcherBuilder.build()), null)
        }.getOrElse { return false }

        return keys.any { jwk ->
            runCatching {
                val publicKey = jwk.toRSAKey().toRSAPublicKey()
                jwt.verify(RSASSAVerifier(publicKey))
            }.getOrDefault(false)
        }
    }
}
