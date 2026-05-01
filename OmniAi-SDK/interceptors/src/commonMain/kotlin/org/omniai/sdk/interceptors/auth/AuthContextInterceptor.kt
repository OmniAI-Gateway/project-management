package org.omniai.sdk.interceptors.auth

import org.omniai.sdk.interceptors.auth.cache.InMemoryKeyCache
import org.omniai.sdk.interceptors.auth.domain.AuthSetupConfig
import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.InterceptorChain
import org.omniai.sdk.core.pipeline.PipelineResult
import org.omniai.sdk.domain.common.AUTH_BEARER_TOKEN_KEY
import org.omniai.sdk.domain.common.AUTH_TOKEN_KIND_KEY
import org.omniai.sdk.domain.errors.InvalidRequest
import org.omniai.sdk.interceptors.auth.domain.AuthenticationDecision
import org.omniai.sdk.interceptors.auth.domain.AuthValidationResult
import org.omniai.sdk.interceptors.auth.domain.TokenValidationParams
import org.omniai.sdk.interceptors.auth.interfaces.TokenAuthenticator
import org.omniai.sdk.interceptors.auth.interfaces.PublicKeyCache
import org.omniai.sdk.interceptors.auth.config.loadTokenAuthenticator
import org.omniai.sdk.interceptors.auth.domain.AuthToken
import org.omniai.sdk.interceptors.auth.domain.DecodedJwt
import org.omniai.sdk.interceptors.auth.domain.JWT
import org.omniai.sdk.interceptors.auth.domain.OPAQUE
import org.omniai.sdk.interceptors.auth.domain.OpaqueToken
import org.omniai.sdk.core.commom.key

fun interface TokenPolicy {
    fun evaluate(token: AuthToken): AuthenticationDecision
}

fun List<TokenPolicy>.evaluateAll(token: AuthToken): AuthenticationDecision.Deny? {
    for (policy in this) {
        when (val res = policy.evaluate(token)) {
            is AuthenticationDecision.Allow -> continue
            is AuthenticationDecision.Deny -> return res
        }
    }
    return null
}

class AuthContextInterceptor(
    private val policies: List<TokenPolicy> = emptyList(),
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
                context.attributes.put(AUTH_RESULT_KEY, decision.data)
                val deny = policies.evaluateAll(token)
                if (deny != null) {
                    return PipelineResult.Error(
                        InvalidRequest("Authentication failed: ${deny.reason}")
                    )
                }
                chain.proceed(context)
            }
            is AuthenticationDecision.Deny -> PipelineResult.Error(
                InvalidRequest("Authentication failed: ${decision.reason}")
            )
        }
    }

    companion object {
        val AUTH_RESULT_KEY = key<AuthValidationResult>("auth_result_key")

        suspend fun build(
            setup: AuthSetupConfig,
            policies: List<TokenPolicy> = emptyList(),
            cache: PublicKeyCache = InMemoryKeyCache()
        ): AuthContextInterceptor {

            val configuredAuth = loadTokenAuthenticator(setup, cache)

            return AuthContextInterceptor(
                policies = policies,
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
