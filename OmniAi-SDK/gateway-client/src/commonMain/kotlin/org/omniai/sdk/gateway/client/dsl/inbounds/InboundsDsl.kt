package org.omniai.sdk.gateway.client.dsl.inbounds

import org.omniai.sdk.core.ports.InboundConnector
import org.omniai.sdk.core.ports.DispatcherPort
import org.omniai.sdk.gateway.client.core.InboundRegistration
import org.omniai.sdk.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniai.sdk.contracts.openai.output.OpenAiChatCompletionsResponse
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.output.AnthropicMessageResponse
import org.omniai.sdk.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniai.sdk.contracts.gemini.output.GeminiGenerateContentResponse
import org.omniai.sdk.core.ports.InboundPort

class InboundsDsl {
    internal var openAiConnector: InboundConnector<OpenAiChatCompletionsRequest, OpenAiChatCompletionsResponse, OpenAiChatCompletionsResponse>? = null
    internal var anthropicConnector: InboundConnector<AnthropicMessagesRequest, AnthropicMessageResponse, org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent>? = null
    internal var geminiConnector: InboundConnector<GeminiGenerateContentRequest, GeminiGenerateContentResponse, GeminiGenerateContentResponse>? = null
    
    internal val customFactories = mutableMapOf<String, CustomInboundSetup>()

    fun openAi(connector: InboundConnector<OpenAiChatCompletionsRequest, OpenAiChatCompletionsResponse, OpenAiChatCompletionsResponse>) {
        openAiConnector = connector
    }

    fun anthropic(connector: InboundConnector<AnthropicMessagesRequest, AnthropicMessageResponse, org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent>) {
        anthropicConnector = connector
    }

    fun gemini(connector: InboundConnector<GeminiGenerateContentRequest, GeminiGenerateContentResponse, GeminiGenerateContentResponse>) {
        geminiConnector = connector
    }

    fun custom(name: String, setup: CustomInboundSetup) {
        customFactories[name] = setup
    }

    internal fun build(): InboundRegistration = InboundRegistration(
        openAiConnector = openAiConnector,
        anthropicConnector = anthropicConnector,
        geminiConnector = geminiConnector,
        customFactories = customFactories.toMap()
    )
}

data class CustomInboundSetup(
    val factory: (DispatcherPort) -> InboundPort<*, *, *>,
    val connect: (InboundPort<*, *, *>) -> Unit
)

