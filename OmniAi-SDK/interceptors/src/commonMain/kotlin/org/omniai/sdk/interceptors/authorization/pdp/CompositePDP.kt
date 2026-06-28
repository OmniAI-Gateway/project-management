package org.omniai.sdk.interceptors.auth.pdp

import org.omniai.sdk.interceptors.auth.AuthorizationDecision
import org.omniai.sdk.interceptors.auth.AuthorizationInput
import org.omniai.sdk.interceptors.auth.interfaces.PolicyDecisionPointPort

class CompositePDP(
    private val delegates: List<PolicyDecisionPointPort>,
) : PolicyDecisionPointPort {
    override suspend fun decide(context: AuthorizationInput): AuthorizationDecision {
        for (pdp in delegates) {
            when (val result = pdp.decide(context)) {
                is AuthorizationDecision.Allow -> continue
                is AuthorizationDecision.Deny -> return result
            }
        }

        return AuthorizationDecision.Allow
    }
}
