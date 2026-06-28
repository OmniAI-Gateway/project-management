package org.omniai.sdk.interceptors.auth

import org.omniai.sdk.application.pipeline.GatewayContext
import org.omniai.sdk.application.pipeline.Interceptor
import org.omniai.sdk.application.pipeline.InterceptorChain
import org.omniai.sdk.application.pipeline.PipelineResult
import org.omniai.sdk.domain.errors.InvalidRequest
import org.omniai.sdk.interceptors.auth.interfaces.PolicyDecisionPointPort

class PolicyEnforcerInterceptor(
    private val inputProvider: AuthorizationInputProvider,
    private val pdp: PolicyDecisionPointPort,
) : Interceptor {
    override suspend fun handle(
        context: GatewayContext,
        chain: InterceptorChain,
    ): PipelineResult =
        when (val decision = pdp.decide(context.toInputProvider(inputProvider))) {
            is AuthorizationDecision.Allow -> {
                chain.proceed(context)
            }

            is AuthorizationDecision.Deny -> {
                PipelineResult.Error(
                    InvalidRequest("Authorization failed: ${decision.reason}"),
                )
            }
        }
}
