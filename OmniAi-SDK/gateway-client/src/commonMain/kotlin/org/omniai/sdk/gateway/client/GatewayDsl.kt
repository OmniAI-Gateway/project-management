package org.omniai.sdk.gateway.client

import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.core.ports.OutboundPort
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.gateway.client.auth.AuthorizationServerConfig
import org.omniai.sdk.gateway.client.auth.AuthorizationServerDsl
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

fun interface MetricsExporter {
    suspend fun export(metric: GatewayMetric, snapshot: TypedMap)
}

data class MetricsConfig(
    val enabled: Boolean,
    val enabledMetrics: Set<GatewayMetric>,
    val exporters: List<MetricsExporter>
)

sealed interface AiServiceSelection {
    data object BuiltIn : AiServiceSelection
    data class Custom(val service: InferenceServicePort) : AiServiceSelection
}

fun gatewayConfig(block: GatewayConfigDsl.() -> Unit): GatewayDefinition =
    GatewayConfigDsl().apply(block).build()

class GatewayConfigDsl {
    private val network = NetworkDsl()
    private val outbounds = OutboundsDsl()
    private val inbounds = InboundsDsl()
    private val interceptors = InterceptorsDsl()
    private val metrics = MetricsDsl()
    private val services = AiServicesDsl()
    private var authorizationServer: AuthorizationServerConfig = AuthorizationServerConfig.None

    fun network(block: NetworkDsl.() -> Unit) {
        network.apply(block)
    }

    fun outbounds(block: OutboundsDsl.() -> Unit) {
        outbounds.apply(block)
    }

    fun inbounds(block: InboundsDsl.() -> Unit) {
        inbounds.apply(block)
    }

    fun interceptors(block: InterceptorsDsl.() -> Unit) {
        interceptors.apply(block)
    }

    fun metrics(block: MetricsDsl.() -> Unit) {
        metrics.apply(block)
    }

    fun services(block: AiServicesDsl.() -> Unit) {
        services.apply(block)
    }

    fun authorizationServer(block: AuthorizationServerDsl.() -> Unit) {
        authorizationServer = AuthorizationServerDsl().apply(block).build()
    }

    internal fun build(): GatewayDefinition = GatewayDefinition(
        networkAdapters = network.adapters.toList(),
        outboundPorts = outbounds.values.toList(),
        inbounds = inbounds.build(),
        interceptors = interceptors.build(),
        metrics = metrics.build(),
        aiServices = services.build(),
        authorizationServer = authorizationServer
    )
}

class NetworkDsl {
    internal val adapters = mutableListOf<GatewayNetworkAdapter>()

    fun use(adapter: GatewayNetworkAdapter) {
        adapters += adapter
    }
}

class OutboundsDsl {
    internal val values = mutableListOf<OutboundPort>()

    fun use(port: OutboundPort) {
        values += port
    }

    operator fun OutboundPort.unaryPlus() {
        use(this)
    }
}

class InboundsDsl {
    var openAi: Boolean = true
    var anthropic: Boolean = true
    var gemini: Boolean = true

    private val factories = mutableMapOf<String, (InferenceServicePort) -> Any>()

    fun custom(name: String, factory: (InferenceServicePort) -> Any) {
        factories[name] = factory
    }

    internal fun build(): InboundRegistration = InboundRegistration(
        installOpenAi = openAi,
        installAnthropic = anthropic,
        installGemini = gemini,
        customFactories = factories.toMap()
    )
}

class InterceptorsDsl {
    private val globalInterceptors = mutableListOf<Interceptor>()
    private val localInterceptors = mutableMapOf<Provider, MutableList<Interceptor>>()

    fun global(interceptor: Interceptor) {
        globalInterceptors += interceptor
    }

    fun local(provider: Provider, interceptor: Interceptor) {
        localInterceptors.getOrPut(provider) { mutableListOf() }.add(interceptor)
    }

    internal fun build(): InterceptorRegistration = InterceptorRegistration(
        global = globalInterceptors.toList(),
        localByProvider = localInterceptors.mapValues { (_, list) -> list.toList() }
    )
}

class MetricsDsl {
    var enabled: Boolean = true
    private val metrics = mutableSetOf(
        GatewayMetric.REQUEST_COUNT,
        GatewayMetric.ERROR_COUNT,
        GatewayMetric.LATENCY,
        GatewayMetric.TOKEN_USAGE
    )
    private val sinks = mutableListOf<MetricsExporter>()

    fun enable(metric: GatewayMetric) {
        metrics += metric
    }

    fun disable(metric: GatewayMetric) {
        metrics -= metric
    }

    fun exportTo(exporter: MetricsExporter) {
        sinks += exporter
    }

    internal fun build(): MetricsConfig = MetricsConfig(
        enabled = enabled,
        enabledMetrics = metrics.toSet(),
        exporters = sinks.toList()
    )
}

class AiServicesDsl {
    private var mode: AiServiceSelection = AiServiceSelection.BuiltIn

    fun builtIn() {
        mode = AiServiceSelection.BuiltIn
    }

    fun custom(service: InferenceServicePort) {
        mode = AiServiceSelection.Custom(service)
    }

    internal fun build(): AiServiceSelection = mode
}

