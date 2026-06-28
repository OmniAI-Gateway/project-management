package org.omniai.sdk.gateway.client.extensions.inbounds

import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.output.AnthropicMessageResponse
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent
import org.omniai.sdk.gateway.client.core.InboundSetup
import org.omniai.sdk.gateway.client.dsl.inbounds.InboundsDsl
import org.omniai.sdk.inbound.anthropic.AnthropicInboundAdapter
import org.omniai.sdk.ports.inbound.InboundConnector

fun InboundsDsl.anthropic(connector: InboundConnector<AnthropicMessagesRequest, AnthropicMessageResponse, AnthropicStreamEvent>) {
    register(
        InboundSetup(
            factory = { dispatcher -> AnthropicInboundAdapter(dispatcher) },
            connect = { adapter -> connector.connect(adapter as AnthropicInboundAdapter) },
        ),
    )
}
