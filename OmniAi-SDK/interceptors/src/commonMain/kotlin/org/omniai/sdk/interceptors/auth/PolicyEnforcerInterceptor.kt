package org.omniai.sdk.interceptors.auth

import org.omniai.sdk.core.pipeline.*
import org.omniai.sdk.domain.errors.InvalidRequest
import org.omniai.sdk.interceptors.auth.domain.AuthenticationDecision
import org.omniai.sdk.interceptors.auth.interfaces.PolicyDecisionPoint

class PolicyEnforcerInterceptor(
    private val pdp: PolicyDecisionPoint
) : Interceptor {
    override suspend fun handle(context: GatewayContext, chain: InterceptorChain): PipelineResult {
        return when (val decision = pdp.decide(context)) {
            is AuthenticationDecision.Allow -> chain.proceed(context)
            is AuthenticationDecision.Deny -> PipelineResult.Error(
                InvalidRequest("Authorization failed: ${decision.reason}")
            )
        }
    }
}
