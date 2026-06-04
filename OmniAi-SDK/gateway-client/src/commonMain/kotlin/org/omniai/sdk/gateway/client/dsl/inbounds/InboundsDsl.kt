package org.omniai.sdk.gateway.client.dsl.inbounds

import org.omniai.sdk.gateway.client.core.InboundRegistration
import org.omniai.sdk.gateway.client.core.InboundSetup
import org.omniai.sdk.gateway.client.dsl.GatewayDsl
import org.omniai.sdk.ports.inbound.DispatcherPort
import org.omniai.sdk.ports.inbound.InboundPort

@GatewayDsl
class InboundsDsl {
    internal val setups = mutableListOf<InboundSetup>()

    fun register(setup: InboundSetup) {
        setups.add(setup)
    }

    fun custom(
        factory: (DispatcherPort) -> InboundPort<*, *, *>,
        connect: (InboundPort<*, *, *>) -> Unit
    ) {
        register(InboundSetup(factory, connect))
    }

    internal fun build(): InboundRegistration = InboundRegistration(
        setups = setups.toList()
    )
}


