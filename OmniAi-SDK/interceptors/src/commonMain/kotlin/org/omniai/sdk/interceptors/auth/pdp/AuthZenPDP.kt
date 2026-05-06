package org.omniai.sdk.interceptors.auth.pdp


import org.omniai.sdk.interceptors.auth.AuthorizationDecision
import org.omniai.sdk.interceptors.auth.AuthorizationInput
import org.omniai.sdk.interceptors.auth.domain.AuthZenClient
import org.omniai.sdk.interceptors.auth.interfaces.PolicyDecisionPointPort

class AuthZenPDP(val client: AuthZenClient) : PolicyDecisionPointPort {
    override suspend fun decide(context: AuthorizationInput): AuthorizationDecision {
        TODO()
    }
}
