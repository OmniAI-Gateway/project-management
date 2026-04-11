package org.omniai.sdk.interceptors.auth

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

actual fun joseJwtVerificationEngine(config: JwtAuthConfig): JwtVerificationEngine {
    return NimbusJoseJwtVerificationEngine(config)
}

private class NimbusJoseJwtVerificationEngine(
    private val config: JwtAuthConfig,
    private val nowProvider: () -> Instant = { Instant.now() }
) : JwtVerificationEngine {

    private val allowedAlgorithm = runCatching { JWSAlgorithm.parse(config.allowedAlgorithm) }
        .getOrElse { JWSAlgorithm.RS256 }

    private val jwkSource = RemoteJWKSet<SecurityContext>(
        URL(config.jwksUrl),
        DefaultResourceRetriever(config.connectTimeoutMillis, config.readTimeoutMillis)
    )

    override suspend fun verify(rawToken: String): AuthenticationDecision {
        val signedJwt = runCatching { SignedJWT.parse(rawToken) }
            .getOrElse { return AuthenticationDecision.Deny("Malformed JWT") }

        val header = signedJwt.header
        if (header.algorithm != allowedAlgorithm) {
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

    private fun verifySignature(jwt: SignedJWT, keyId: String?): Boolean {
        val keys = runCatching {
            val matcherBuilder = JWKMatcher.Builder()
                .keyType(KeyType.RSA)
                .algorithm(allowedAlgorithm)
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

