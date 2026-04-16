package org.omniai.sdk.interceptors.auth

import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.InterceptorChain
import org.omniai.sdk.core.pipeline.PipelineResult
import org.omniai.sdk.domain.common.AUTH_BEARER_TOKEN_KEY
import org.omniai.sdk.domain.common.AUTH_TOKEN_KIND_KEY
import org.omniai.sdk.domain.errors.InvalidRequest

enum class TokenKind {
	JWT,
	OPAQUE
}

data class AuthToken(
	val rawValue: String,
	val kind: TokenKind
)

interface TokenKindDetector {
	fun detect(rawToken: String): TokenKind
}

class DotSegmentsTokenKindDetector : TokenKindDetector {
	override fun detect(rawToken: String): TokenKind {
		val dotSegments = rawToken.split('.')
		return if (dotSegments.size == 3 && dotSegments.none { it.isBlank() }) {
			TokenKind.JWT
		} else {
			TokenKind.OPAQUE
		}
	}
}

sealed interface AuthenticationDecision {
	data class Allow(val claims: Map<String, Any>) : AuthenticationDecision
	data class Deny(val reason: String) : AuthenticationDecision
}

interface TokenAuthenticator {
	suspend fun authenticate(token: AuthToken, context: GatewayContext): AuthenticationDecision
}

class PassThroughTokenAuthenticator : TokenAuthenticator {
	override suspend fun authenticate(token: AuthToken, context: GatewayContext): AuthenticationDecision {
		return AuthenticationDecision.Allow(claims = emptyMap())
	}
}

class AuthContextInterceptor(
	private val detector: TokenKindDetector = DotSegmentsTokenKindDetector(),
	private val authenticator: TokenAuthenticator = PassThroughTokenAuthenticator()
) : Interceptor {

	override suspend fun handle(context: GatewayContext, chain: InterceptorChain): PipelineResult {
		val bearerToken = context.request.providerOptions[AUTH_BEARER_TOKEN_KEY] as? String
		if (bearerToken.isNullOrBlank()) {
			return chain.proceed(context)
		}

		val authToken = AuthToken(
			rawValue = bearerToken,
			kind = detector.detect(bearerToken)
		)

		context.attributes.put(AUTH_TOKEN_KIND_KEY, authToken.kind.name)

		return when (val decision = authenticator.authenticate(authToken, context)) {
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
}

