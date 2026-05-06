package org.omniai.sdk.interceptors.auth.pdp

import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.interceptors.auth.domain.AuthZenClient
import org.omniai.sdk.interceptors.auth.domain.AuthenticationDecision
import org.omniai.sdk.interceptors.auth.interfaces.PolicyDecisionPoint

class AuthZenPDP(val client: AuthZenClient) : PolicyDecisionPoint {
    override suspend fun decide(context: GatewayContext): AuthenticationDecision {
        TODO()
    }
}
