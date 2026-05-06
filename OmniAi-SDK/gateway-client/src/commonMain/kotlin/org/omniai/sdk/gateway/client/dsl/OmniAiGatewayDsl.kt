package org.omniai.sdk.gateway.client.dsl

import org.omniai.sdk.gateway.client.auth.AuthorizationServerConfig
import org.omniai.sdk.gateway.client.auth.AuthorizationServerDsl
import org.omniai.sdk.gateway.client.auth.SecurityConfig
import org.omniai.sdk.gateway.client.auth.SecurityDsl
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.gateway.client.core.OmniAiConfig
import org.omniai.sdk.gateway.client.core.ExecutionMode
import org.omniai.sdk.gateway.client.dsl.inbounds.InboundsDsl
import org.omniai.sdk.gateway.client.dsl.outbounds.OutboundsDsl
import org.omniai.sdk.gateway.client.dsl.interceptors.InterceptorsDsl

fun omniAiGateway(block: OmniAiGatewayDsl.() -> Unit): OmniAiConfig =
    OmniAiGatewayDsl().apply(block).build()

class OmniAiGatewayDsl {
    private val inboundsDsl = InboundsDsl()
    private var executionMode: ExecutionMode? = null
    private var securityConfig: SecurityConfig = SecurityConfig()

    fun inbounds(block: InboundsDsl.() -> Unit) {
        inboundsDsl.apply(block)
    }

    fun execution(block: ExecutionDsl.() -> Unit) {
        executionMode = ExecutionDsl().apply(block).build()
    }

    fun security(block: SecurityDsl.() -> Unit) {
        securityConfig = SecurityDsl().apply(block).build()
    }

    fun build(): OmniAiConfig {
        val resolvedExecution = requireNotNull(executionMode) {
            "You must define an execution block with either useNativePipeline or useCustomService."
        }
        return OmniAiConfig(
            inbounds = inboundsDsl.build(),
            execution = resolvedExecution,
            security = securityConfig
        )
    }
}

class ExecutionDsl {
    private var executionMode: ExecutionMode? = null

    fun useNativePipeline(block: NativePipelineDsl.() -> Unit) {
        require(executionMode == null) { "You can only define one execution mode." }
        executionMode = NativePipelineDsl().apply(block).build()
    }

    fun useCustomService(service: InferenceServicePort) {
        require(executionMode == null) { "You can only define one execution mode." }
        executionMode = ExecutionMode.CustomService(service)
    }

    internal fun build(): ExecutionMode = requireNotNull(executionMode) {
        "You must choose an execution mode: useNativePipeline or useCustomService."
    }
}

class NativePipelineDsl {
    private val outboundsDsl = OutboundsDsl()
    private val interceptorsDsl = InterceptorsDsl()

    fun outbounds(block: OutboundsDsl.() -> Unit) {
        outboundsDsl.apply(block)
    }

    fun interceptors(block: InterceptorsDsl.() -> Unit) {
        interceptorsDsl.apply(block)
    }

    internal fun build(): ExecutionMode.NativePipeline = ExecutionMode.NativePipeline(
        outbounds = outboundsDsl.build(),
        interceptors = interceptorsDsl.build()
    )
}
