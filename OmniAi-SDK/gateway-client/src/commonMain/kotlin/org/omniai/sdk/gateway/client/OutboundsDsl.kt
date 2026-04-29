package org.omniai.sdk.gateway.client

import org.omniai.sdk.core.ports.OutboundPort

class OutboundsDsl {
    internal val values = mutableListOf<OutboundPort>()

    fun use(port: OutboundPort) {
        values += port
    }

    operator fun OutboundPort.unaryPlus() {
        use(this)
    }
}

