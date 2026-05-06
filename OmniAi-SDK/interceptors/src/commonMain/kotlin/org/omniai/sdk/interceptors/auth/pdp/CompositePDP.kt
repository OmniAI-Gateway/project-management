package org.omniai.sdk.interceptors.auth.pdp

import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.interceptors.auth.AUTH_RESULT_KEY
import org.omniai.sdk.interceptors.auth.domain.AuthenticationDecision
import org.omniai.sdk.interceptors.auth.interfaces.PolicyDecisionPoint

class CompositePDP(
    private val delegates: List<PolicyDecisionPoint>
) : PolicyDecisionPoint {
    override suspend fun decide(context: GatewayContext): AuthenticationDecision {
        for (pdp in delegates) {
            when (val result = pdp.decide(context)) {
                is AuthenticationDecision.Allow -> continue
                is AuthenticationDecision.Deny -> return result
            }
        }
        val authResult = context.attributes[AUTH_RESULT_KEY]
            ?: return AuthenticationDecision.Deny("Authentication result not found")

        return AuthenticationDecision.Allow(authResult)
    }
}