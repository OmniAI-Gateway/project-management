package org.omniai.sdk.gateway.client
import org.omniai.sdk.gateway.client.auth.AuthorizationServerConfig
import org.omniai.sdk.gateway.client.auth.AuthorizationServerDsl
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

