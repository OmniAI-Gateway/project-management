package org.omniai.sdk.auth

import org.omniai.sdk.auth.cache.InMemoryKeyCache
import org.omniai.sdk.auth.domain.AuthSetupConfig
import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.InterceptorChain
import org.omniai.sdk.core.pipeline.PipelineResult
import org.omniai.sdk.domain.common.AUTH_BEARER_TOKEN_KEY
import org.omniai.sdk.domain.common.AUTH_TOKEN_KIND_KEY
import org.omniai.sdk.domain.errors.InvalidRequest
import org.omniai.sdk.auth.domain.AuthToken
import org.omniai.sdk.auth.domain.AuthenticationDecision
import org.omniai.sdk.auth.domain.TokenValidationParams
import org.omniai.sdk.auth.interfaces.TokenAuthenticator
import org.omniai.sdk.auth.domain.detectTokenKind
import org.omniai.sdk.auth.interfaces.PublicKeyCache


class AuthContextInterceptor(
    private val authenticator: TokenAuthenticator,
    private val validationParams: TokenValidationParams
) : Interceptor {

    override suspend fun handle(context: GatewayContext, chain: InterceptorChain): PipelineResult {
        val bearerToken = context.request.providerOptions[AUTH_BEARER_TOKEN_KEY] as? String

        if (bearerToken.isNullOrBlank()) {
            return chain.proceed(context)
        }

        val authToken = AuthToken(
            rawValue = bearerToken,
            kind = detectTokenKind(bearerToken)
        )

        context.attributes.put(AUTH_TOKEN_KIND_KEY, authToken.kind.name)

        return when (val decision = authenticator.authenticate(authToken, validationParams)) {
            is AuthenticationDecision.Allow -> {
                decision.claims["sub"]?.let { userId ->
                    context.attributes.put("auth_user_id", userId.toString())
                }
                context.attributes.put("auth_claims", decision.claims)
                chain.proceed(context)
            }
            is AuthenticationDecision.Deny -> PipelineResult.Error(
                InvalidRequest("Authentication failed: ${decision.reason}")
            )
        }
    }
    companion object {
        suspend fun build(
            setup: AuthSetupConfig,
            cache: PublicKeyCache = InMemoryKeyCache()
        ): AuthContextInterceptor {

            val configuredAuth = loadTokenAuthenticator(setup, cache)

            return AuthContextInterceptor(
                authenticator = configuredAuth.authenticator,
                validationParams = configuredAuth.validationParams
            )
        }
    }
}
