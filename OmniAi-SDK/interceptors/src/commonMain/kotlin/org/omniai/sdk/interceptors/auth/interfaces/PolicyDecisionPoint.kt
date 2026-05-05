package org.omniai.sdk.interceptors.auth.interfaces

import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.interceptors.auth.domain.AuthenticationDecision

interface PolicyDecisionPoint {
    suspend fun decide(context: GatewayContext): AuthenticationDecision
}
