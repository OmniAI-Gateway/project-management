package org.omniai.sdk.gateway.client.auth

import org.omniai.sdk.application.pipeline.GatewayContext
import org.omniai.sdk.interceptors.auth.Action
import org.omniai.sdk.interceptors.auth.AuthorizationInput
import org.omniai.sdk.interceptors.auth.AuthorizationInputProvider
import org.omniai.sdk.interceptors.auth.Resource
import org.omniai.sdk.interceptors.auth.Subject
import org.omniai.sdk.interceptors.auth.interfaces.PolicyDecisionPointPort
import org.omniai.sdk.interceptors.auth.pdp.PassThroughPDP

data class SecurityConfig(
    val authentication: AuthorizationServerConfig = AuthorizationServerConfig.None,
    val authorization: AuthorizationConfig = AuthorizationConfig.None,
)

data class AuthorizationConfig(
    val pdp: PolicyDecisionPointPort,
    val inputProvider: AuthorizationInputProvider,
) {
    companion object {
        val None =
            AuthorizationConfig(
                pdp = PassThroughPDP(),
                inputProvider = DefaultAuthorizationInputProvider,
            )
    }
}

object DefaultAuthorizationInputProvider : AuthorizationInputProvider {
    override fun getAuthorizationInput(context: GatewayContext): AuthorizationInput =
        AuthorizationInput(
            subject = Subject(id = "anonymous", roles = emptyList()),
            action = Action(name = "inference"),
            resource = Resource(type = "model", id = context.request.model),
        )
}

class SecurityDsl {
    private var authenticationConfig: AuthorizationServerConfig = AuthorizationServerConfig.None
    private var authorizationConfig: AuthorizationConfig = AuthorizationConfig.None

    fun authentication(block: AuthorizationServerDsl.() -> Unit) {
        authenticationConfig = AuthorizationServerDsl().apply(block).build()
    }

    fun authorization(block: AuthorizationDsl.() -> Unit) {
        authorizationConfig = AuthorizationDsl().apply(block).build()
    }

    internal fun build(): SecurityConfig = SecurityConfig(authenticationConfig, authorizationConfig)
}

class AuthorizationDsl {
    var pdp: PolicyDecisionPointPort = PassThroughPDP()
    var inputProvider: AuthorizationInputProvider = DefaultAuthorizationInputProvider

    internal fun build(): AuthorizationConfig = AuthorizationConfig(pdp, inputProvider)
}
