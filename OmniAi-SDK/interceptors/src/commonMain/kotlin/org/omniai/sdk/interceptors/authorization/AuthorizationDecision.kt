package org.omniai.sdk.interceptors.auth

sealed interface AuthorizationDecision {
    data object Allow : AuthorizationDecision

    data class Deny(
        val reason: String?,
    ) : AuthorizationDecision
}
