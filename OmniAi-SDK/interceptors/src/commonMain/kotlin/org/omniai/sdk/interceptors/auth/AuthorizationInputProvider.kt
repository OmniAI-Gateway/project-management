package org.omniai.sdk.interceptors.auth

import org.omniai.sdk.application.pipeline.GatewayContext

fun interface AuthorizationInputProvider {
    fun getAuthorizationInput(context: GatewayContext): AuthorizationInput
}

fun GatewayContext.toInputProvider(engine: AuthorizationInputProvider): AuthorizationInput {
    return engine.getAuthorizationInput(this)
}

data class AuthorizationInput(
    val subject: Subject,
    val action: Action,
    val resource: Resource,
    val context: Map<String, Any> = emptyMap()
)

data class Subject(
    val id: String,
    val roles: List<String> = emptyList(),
    val attributes: Map<String, Any> = emptyMap()
)
data class Action(
    val name: String
)

data class Resource(
    val type: String,
    val id: String? = null
)