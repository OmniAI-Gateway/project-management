package org.omniai.sdk.gateway.client.dsl.interceptors

import org.omniai.sdk.common.key
import org.omniai.sdk.common.AttributeKey
import org.omniai.sdk.application.pipeline.Interceptor
import org.omniai.sdk.ports.outbound.OutboundPort
import org.omniai.sdk.interceptors.circuitBreaker.CircuitBreakerConfig
import org.omniai.sdk.interceptors.circuitBreaker.CircuitBreakerInterceptor
import org.omniai.sdk.interceptors.circuitBreaker.CircuitBreakerStore
import org.omniai.sdk.interceptors.circuitBreaker.InMemoryCircuitBreakerStore
import org.omniai.sdk.interceptors.fallback.FallbackInterceptor
import org.omniai.sdk.interceptors.metrics.MetricsPort
import org.omniai.sdk.interceptors.routing.RoutingInterceptor
import org.omniai.sdk.interceptors.mcpBroker.McpBrokerInterceptor
import io.modelcontextprotocol.kotlin.sdk.client.Client

import org.omniai.sdk.gateway.client.dsl.GatewayDsl

val DefaultDeniedOutboundsKey = key<Set<String>>("denied_outbounds")

@GatewayDsl
class InterceptorsDsl {
    private val interceptors = mutableListOf<Interceptor>()
    private val deferredInterceptors = mutableListOf<(List<OutboundPort>) -> Interceptor>()

    fun use(interceptor: Interceptor) {
        interceptors += interceptor
    }

    fun use(deferred: (List<OutboundPort>) -> Interceptor) {
        deferredInterceptors += deferred
    }

    /**
     * Installs telemetry metrics interceptors based on configuration.
     */
    fun metrics(block: MetricsInterceptorBuilder.() -> Unit) {
        metricsInterceptorBuild(block).forEach(::use)
    }

    /**
     * Configures and installs a RateLimitInterceptor.
     */
    fun rateLimiting(block: RateLimitingInterceptorBuilder.() -> Unit) {
        use(RateLimitingInterceptorBuilder().apply(block).build())
    }

    /**
     * Configures and installs a CircuitBreakerInterceptor.
     */
    fun circuitBreaker(block: CircuitBreakerBuilder.() -> Unit = {}) {
        val builder = CircuitBreakerBuilder().apply(block)
        use { outbounds -> builder.build(outbounds) }
    }

    /**
     * Configures and installs a RoutingInterceptor.
     */
    fun routing() {
        use { outbounds -> RoutingInterceptor(outbounds) }
    }

    /**
     * Configures and installs a FallbackInterceptor.
     */
    fun fallback(metricsPort: MetricsPort? = null) {
        use { outbounds -> FallbackInterceptor(outbounds, DefaultDeniedOutboundsKey, metricsPort) }
    }

    /**
     * Installs the [McpBrokerInterceptor].
     *
     * This interceptor is responsible for:
     * 1. Fetching available tools from the MCP Broker and injecting them into the LLM request.
     * 2. Detecting when the LLM wants to call a tool (FinishReason.TOOL_CALL).
     * 3. Executing the tool via the MCP Broker and continuing the conversation with the result.
     *
     * @param mcpClient The MCP [Client] connected to the in-process MCP Gateway Server.
     */
    fun mcpBroker(mcpClient: Client) {
        use(McpBrokerInterceptor(mcpClient))
    }

    internal fun build(outbounds: List<OutboundPort>): List<Interceptor> = 
        interceptors.toList() + deferredInterceptors.map { it(outbounds) }
}

@GatewayDsl
class CircuitBreakerBuilder {
    var store: CircuitBreakerStore = InMemoryCircuitBreakerStore()
    var config: CircuitBreakerConfig = CircuitBreakerConfig()
    var deniedOutboundsKey: AttributeKey<Set<String>> = DefaultDeniedOutboundsKey
    var metricsPort: MetricsPort? = null

    internal fun build(outbounds: List<OutboundPort>): CircuitBreakerInterceptor {
        return CircuitBreakerInterceptor(store, config, deniedOutboundsKey, outbounds, metricsPort)
    }
}
