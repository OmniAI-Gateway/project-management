package org.omniai.sdk.interceptors.auth

import org.omniai.sdk.application.pipeline.*
import org.omniai.sdk.domain.errors.InvalidRequest
import org.omniai.sdk.interceptors.auth.interfaces.PolicyDecisionPointPort

class PolicyEnforcerInterceptor(
    private val inputProvider: AuthorizationInputProvider,
    private val pdp: PolicyDecisionPointPort
) : Interceptor {
    override suspend fun handle(context: GatewayContext, chain: InterceptorChain): PipelineResult {
        return when (val decision = pdp.decide(context.toInputProvider(inputProvider))) {
            is AuthorizationDecision.Allow -> chain.proceed(context)
            is AuthorizationDecision.Deny -> PipelineResult.Error(
                InvalidRequest("Authorization failed: ${decision.reason}")
            )
        }
    }
}
