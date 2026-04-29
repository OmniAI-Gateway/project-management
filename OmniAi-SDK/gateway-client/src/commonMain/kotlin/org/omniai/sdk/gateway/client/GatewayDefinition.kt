package org.omniai.sdk.gateway.client

import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.core.ports.OutboundPort
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.gateway.client.auth.AuthorizationServerConfig
import org.omniai.sdk.inbound.anthropic.AnthropicInboundAdapter
import org.omniai.sdk.inbound.gemini.GeminiInboundAdapter
import org.omniai.sdk.inbound.openai.OpenAiInboundAdapter

/**
 * Final immutable configuration produced by the Gateway DSL.
 */
data class GatewayDefinition(
    val networkAdapters: List<GatewayNetworkAdapter>,
    val outboundPorts: List<OutboundPort>,
    val inbounds: InboundRegistration,
    val interceptors: InterceptorRegistration,
    val metrics: MetricsConfig,
    val aiServices: AiServiceSelection,
    val authorizationServer: AuthorizationServerConfig
)

/**
 * Runtime graph returned after assembly on the host platform.
 */
data class GatewayRuntime(
    val service: InferenceServicePort,
    val inbounds: GatewayInboundAdapters,
    val metadata: TypedMap = TypedMap()
)

/**
 * Connection point for transport adapters (Ktor, custom HTTP server, etc).
 */
fun interface GatewayNetworkAdapter {
    suspend fun connect(runtime: GatewayRuntime)
}

data class GatewayInboundAdapters(
    val openAi: OpenAiInboundAdapter?,
    val anthropic: AnthropicInboundAdapter?,
    val gemini: GeminiInboundAdapter?,
    val custom: Map<String, Any> = emptyMap()
)

data class InboundRegistration(
    val installOpenAi: Boolean,
    val installAnthropic: Boolean,
    val installGemini: Boolean,
    val customFactories: Map<String, (InferenceServicePort) -> Any>
)

data class InterceptorRegistration(
    val global: List<Interceptor>,
    val localByProvider: Map<Provider, List<Interceptor>>
)

enum class GatewayMetric {
    REQUEST_COUNT,
    ERROR_COUNT,
    LATENCY,
    TOKEN_USAGE
}

data class MetricsConfig(
    val enabled: Boolean,
    val enabledMetrics: Set<GatewayMetric>,
    val interceptors: List<Interceptor>
)

sealed interface AiServiceSelection {
    data object BuiltIn : AiServiceSelection
    data class Custom(val service: InferenceServicePort) : AiServiceSelection
}

