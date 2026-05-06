package org.omniai.sdk.interceptors.auth

import org.omniai.sdk.core.commom.key
import org.omniai.sdk.core.pipeline.*
import org.omniai.sdk.domain.common.AUTH_BEARER_TOKEN_KEY
import org.omniai.sdk.domain.errors.InvalidRequest
import org.omniai.sdk.interceptors.auth.cache.InMemoryKeyCache
import org.omniai.sdk.interceptors.auth.config.loadTokenAuthenticator
import org.omniai.sdk.interceptors.auth.domain.*
import org.omniai.sdk.interceptors.auth.interfaces.IntrospectionCache
import org.omniai.sdk.interceptors.auth.interfaces.PublicKeyCache
import org.omniai.sdk.interceptors.auth.interfaces.TokenAuthenticator
import kotlin.time.Duration

class AuthenticationInterceptor(
    private val authenticator: TokenAuthenticator,
    private val validationParams: TokenValidationParams? = null
) : Interceptor {

    override suspend fun handle(context: GatewayContext, chain: InterceptorChain): PipelineResult {
        val bearerToken = context.request.providerOptions[AUTH_BEARER_TOKEN_KEY] as? String

        if (bearerToken.isNullOrBlank()) {
            return PipelineResult.Error(
                InvalidRequest("Authentication failed: Token missing")
            )
        }

        val token = if (isJwt(bearerToken)) JWT(DecodedJwt.decode(bearerToken))
        else OPAQUE(OpaqueToken(bearerToken))

        context.attributes.put(AUTH_TOKEN_KIND_KEY, token::class.simpleName ?: "no Name")

        return when (val decision = authenticator.authenticate(token, validationParams)) {
            is AuthenticationDecision.Allow -> {
                context.attributes.put(AUTH_RESULT_KEY, decision.data)
                context.attributes.put(AUTH_TOKEN_KEY, token)
                chain.proceed(context)
            }
            is AuthenticationDecision.Deny -> PipelineResult.Error(
                InvalidRequest("Authentication failed: ${decision.reason}")
            )
        }
    }

    companion object {
        val AUTH_TOKEN_KEY = key<AuthToken>("auth_token")

        suspend fun build(
            setup: AuthSetupConfig,
            publicKeyCache: PublicKeyCache = InMemoryKeyCache(),
            introspectionCache: IntrospectionCache? = null,
            positiveCacheTtl: Duration? = null,
            negativeCacheTtl: Duration? = null,
        ): AuthenticationInterceptor {
            val configuredAuth = loadTokenAuthenticator(
                config = setup,
                publicKeyCache = publicKeyCache,
                introspectionCache = introspectionCache,
                positiveCacheTtl = positiveCacheTtl,
                negativeCacheTtl = negativeCacheTtl,
            )

            return AuthenticationInterceptor(
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
