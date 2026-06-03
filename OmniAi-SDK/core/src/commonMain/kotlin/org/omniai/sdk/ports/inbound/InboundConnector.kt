package org.omniai.sdk.ports.inbound

/**
 * Functional interface used to connect an external web framework (like Ktor or Spring)
 * to a specific [InboundPort] after the Gateway pipeline has been fully assembled.
 */
fun interface InboundConnector<Req, Res, Event> {
    fun connect(port: InboundPort<Req, Res, Event>)
}
