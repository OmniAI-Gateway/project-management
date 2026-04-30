package org.omniai.sdk.interceptors.auth

import org.omniai.sdk.auth.cache.InMemoryKeyCache
import org.omniai.sdk.auth.domain.AuthSetupConfig
import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.InterceptorChain
import org.omniai.sdk.core.pipeline.PipelineResult
import org.omniai.sdk.domain.common.AUTH_BEARER_TOKEN_KEY
import org.omniai.sdk.domain.common.AUTH_TOKEN_KIND_KEY
import org.omniai.sdk.domain.errors.InvalidRequest
import org.omniai.sdk.auth.domain.AuthenticationDecision
import org.omniai.sdk.auth.domain.TokenValidationParams
import org.omniai.sdk.auth.interfaces.TokenAuthenticator
import org.omniai.sdk.auth.interfaces.PublicKeyCache
import org.omniai.sdk.auth.config.loadTokenAuthenticator
import org.omniai.sdk.auth.domain.DecodedJwt
import org.omniai.sdk.auth.domain.JWT
import org.omniai.sdk.auth.domain.OPAQUE
import org.omniai.sdk.auth.domain.OpaqueToken


class AuthContextInterceptor(
    private val authenticator: TokenAuthenticator,
    private val validationParams: TokenValidationParams?
) : Interceptor {

    override suspend fun handle(context: GatewayContext, chain: InterceptorChain): PipelineResult {
        val bearerToken = context.request.providerOptions[AUTH_BEARER_TOKEN_KEY] as? String

        if (bearerToken.isNullOrBlank()) {
            return chain.proceed(context)
        }

        val token = if (isJwt(bearerToken)) JWT(DecodedJwt.decode(bearerToken))
        else OPAQUE(OpaqueToken(bearerToken))

        context.attributes.put(AUTH_TOKEN_KIND_KEY, token::class.simpleName ?: "no Name")

        return when (val decision = authenticator.authenticate(token, validationParams)) {
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

    private fun isJwt(token: String): Boolean {
        val segments = token.split('.')
        return segments.size == 3 && segments.none { it.isBlank() }
    }
}

