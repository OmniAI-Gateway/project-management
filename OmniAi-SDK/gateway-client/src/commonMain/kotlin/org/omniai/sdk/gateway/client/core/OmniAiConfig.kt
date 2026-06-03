package org.omniai.sdk.gateway.client.core

import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.application.pipeline.Interceptor
import org.omniai.sdk.ports.inbound.InboundConnector
import org.omniai.sdk.ports.inbound.DispatcherPort
import org.omniai.sdk.ports.outbound.OutboundPort
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.gateway.client.auth.AuthorizationServerConfig
import org.omniai.sdk.gateway.client.auth.SecurityConfig
import org.omniai.sdk.inbound.anthropic.AnthropicInboundAdapter
import org.omniai.sdk.inbound.gemini.GeminiInboundAdapter
import org.omniai.sdk.inbound.openai.OpenAiInboundAdapter
import org.omniai.sdk.gateway.client.dsl.inbounds.CustomInboundSetup

import org.omniai.sdk.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniai.sdk.contracts.openai.output.OpenAiChatCompletionsResponse
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.output.AnthropicMessageResponse
import org.omniai.sdk.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniai.sdk.contracts.gemini.output.GeminiGenerateContentResponse

/**
 * Final immutable configuration produced by the OmniAi DSL.
 */
data class OmniAiConfig(
    val inbounds: InboundRegistration,
    val execution: ExecutionMode,
    val security: SecurityConfig
)

/**
 * Runtime graph returned after assembly on the host platform.
 */
data class OmniAiRuntime(
    val dispatcher: DispatcherPort,
    val metadata: TypedMap = TypedMap()
)

data class InboundRegistration(
    val openAiConnector: InboundConnector<OpenAiChatCompletionsRequest, OpenAiChatCompletionsResponse, OpenAiChatCompletionsResponse>?,
    val anthropicConnector: InboundConnector<AnthropicMessagesRequest, AnthropicMessageResponse, org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent>?,
    val geminiConnector: InboundConnector<GeminiGenerateContentRequest, GeminiGenerateContentResponse, GeminiGenerateContentResponse>?,
    val customFactories: Map<String, CustomInboundSetup>
)

sealed interface ExecutionMode {
    data class NativePipeline(
        val outbounds: List<OutboundPort>,
        val interceptors: List<Interceptor>
    ) : ExecutionMode
    data class CustomDispatcher(val dispatcher: DispatcherPort) : ExecutionMode
}
