package org.omniai.sdk.ports.inbound

fun interface InboundConnector<Req, Res, Event> {
    fun connect(port: InboundPort<Req, Res, Event>)
}
