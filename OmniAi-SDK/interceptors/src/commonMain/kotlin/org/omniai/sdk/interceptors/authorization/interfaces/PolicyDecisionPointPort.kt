package org.omniai.sdk.interceptors.auth.interfaces

import org.omniai.sdk.interceptors.auth.AuthorizationDecision
import org.omniai.sdk.interceptors.auth.AuthorizationInput

interface PolicyDecisionPointPort {
    suspend fun decide(context: AuthorizationInput): AuthorizationDecision
}
