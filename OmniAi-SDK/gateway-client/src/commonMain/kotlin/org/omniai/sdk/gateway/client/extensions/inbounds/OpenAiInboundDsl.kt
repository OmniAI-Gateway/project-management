package org.omniai.sdk.gateway.client.extensions.inbounds

import org.omniai.sdk.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniai.sdk.contracts.openai.output.OpenAiChatCompletionsResponse
import org.omniai.sdk.gateway.client.core.InboundSetup
import org.omniai.sdk.gateway.client.dsl.inbounds.InboundsDsl
import org.omniai.sdk.inbound.openai.OpenAiInboundAdapter
import org.omniai.sdk.ports.inbound.InboundConnector

fun InboundsDsl.openAi(
    connector: InboundConnector<OpenAiChatCompletionsRequest, OpenAiChatCompletionsResponse, OpenAiChatCompletionsResponse>,
) {
    register(
        InboundSetup(
            factory = { dispatcher -> OpenAiInboundAdapter(dispatcher) },
            connect = { adapter -> connector.connect(adapter as OpenAiInboundAdapter) },
        ),
    )
}
