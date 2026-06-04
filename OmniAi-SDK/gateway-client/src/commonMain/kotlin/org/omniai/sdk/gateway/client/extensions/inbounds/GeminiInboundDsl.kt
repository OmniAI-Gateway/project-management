package org.omniai.sdk.gateway.client.extensions.inbounds

import org.omniai.sdk.gateway.client.core.InboundSetup
import org.omniai.sdk.gateway.client.dsl.inbounds.InboundsDsl
import org.omniai.sdk.inbound.gemini.GeminiInboundAdapter
import org.omniai.sdk.ports.inbound.InboundConnector
import org.omniai.sdk.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniai.sdk.contracts.gemini.output.GeminiGenerateContentResponse

fun InboundsDsl.gemini(
    connector: InboundConnector<GeminiGenerateContentRequest, GeminiGenerateContentResponse, GeminiGenerateContentResponse>
) {
    register(InboundSetup(
        factory = { dispatcher -> GeminiInboundAdapter(dispatcher) },
        connect = { adapter -> connector.connect(adapter as GeminiInboundAdapter) }
    ))
}
