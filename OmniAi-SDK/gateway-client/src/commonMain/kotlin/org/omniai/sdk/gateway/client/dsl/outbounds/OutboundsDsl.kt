package org.omniai.sdk.gateway.client.dsl.outbounds

import org.omniai.sdk.gateway.client.dsl.GatewayDsl
import org.omniai.sdk.ports.outbound.OutboundPort

@GatewayDsl
class OutboundsDsl {
    internal val values = mutableListOf<OutboundPort>()

    fun use(port: OutboundPort) {
        values += port
    }

    operator fun OutboundPort.unaryPlus() {
        use(this)
    }

    internal fun build(): List<OutboundPort> = values.toList()
}
