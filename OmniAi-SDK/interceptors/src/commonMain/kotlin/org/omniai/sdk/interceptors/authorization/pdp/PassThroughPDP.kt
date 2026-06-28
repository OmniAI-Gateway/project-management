package org.omniai.sdk.interceptors.auth.pdp

import org.omniai.sdk.interceptors.auth.AuthorizationDecision
import org.omniai.sdk.interceptors.auth.AuthorizationInput
import org.omniai.sdk.interceptors.auth.interfaces.PolicyDecisionPointPort

class PassThroughPDP : PolicyDecisionPointPort {
    override suspend fun decide(context: AuthorizationInput): AuthorizationDecision {
        return AuthorizationDecision.Allow
    }
}