package org.omniai.sdk.gateway.client.core

import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.application.pipeline.Interceptor
import org.omniai.sdk.ports.inbound.InboundConnector
import org.omniai.sdk.ports.inbound.DispatcherPort
import org.omniai.sdk.ports.outbound.OutboundPort
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.gateway.client.auth.AuthorizationServerConfig
import org.omniai.sdk.gateway.client.auth.SecurityConfig
import org.omniai.sdk.ports.inbound.InboundPort

/**
 * Final immutable configuration produced by the OmniAi DSL.
 */
data class OmniAiConfig(
    val inbounds: InboundRegistration,
    val execution: ExecutionMode,
    val security: SecurityConfig
)

/**
 * Runtime graph returned after assembly on the host platform.
 */
data class OmniAiRuntime(
    val dispatcher: DispatcherPort,
    val metadata: TypedMap = TypedMap()
)

data class InboundRegistration(
    val setups: List<InboundSetup>
)

data class InboundSetup(
    val factory: (DispatcherPort) -> InboundPort<*, *, *>,
    val connect: (InboundPort<*, *, *>) -> Unit
)

sealed interface ExecutionMode {
    data class NativePipeline(
        val outbounds: List<OutboundPort>,
        val interceptors: List<Interceptor>
    ) : ExecutionMode
    data class CustomDispatcher(val dispatcher: DispatcherPort) : ExecutionMode
}
