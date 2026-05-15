package org.omniai.sdk.gateway.client.dsl

import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.ports.InboundConnector
import org.omniai.sdk.core.ports.DispatcherPort
import org.omniai.sdk.core.ports.OutboundPort
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.output.AnthropicMessageResponse
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent
import org.omniai.sdk.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniai.sdk.contracts.gemini.output.GeminiGenerateContentResponse
import org.omniai.sdk.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniai.sdk.contracts.openai.output.OpenAiChatCompletionsResponse
import org.omniai.sdk.gateway.client.core.OmniAiConfig
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * JS-compatible wrapper for OmniAiConfig.
 * This class wraps the Kotlin-native OmniAiConfig so it can be safely used
 * and passed around in a JS environment without exposing unexportable types directly.
 */
@JsExport
class JsOmniAiConfig internal constructor(
    internal val config: OmniAiConfig
)

/**
 * Entry point for pure JS to create an OmniAi Gateway configuration.
 */
@JsExport
@JsName("omniAiGateway")
fun omniAiGateway(setup: (JsOmniAiGatewayBuilder) -> Unit): JsOmniAiConfig {
    val builder = JsOmniAiGatewayBuilder()
    setup(builder)
    return builder.build()
}

@JsExport
@JsName("OmniAiGatewayBuilder")
class JsOmniAiGatewayBuilder {
    private val dsl = OmniAiGatewayDsl()

    fun inbounds(setup: (JsInboundsBuilder) -> Unit): JsOmniAiGatewayBuilder {
        val builder = JsInboundsBuilder()
        setup(builder)
        dsl.inbounds {
            builder.openAiConnector?.let { openAi(it) }
            builder.anthropicConnector?.let { anthropic(it) }
            builder.geminiConnector?.let { gemini(it) }
        }
        return this
    }

    fun execution(setup: (JsExecutionBuilder) -> Unit): JsOmniAiGatewayBuilder {
        val builder = JsExecutionBuilder()
        setup(builder)
        dsl.execution {
            if (builder.isNativePipeline) {
                useNativePipeline {
                    outbounds {
                        builder.outboundsList.forEach { use(it) }
                    }
                    interceptors {
                        builder.interceptorsList.forEach { use(it) }
                    }
                }
            } else if (builder.customDispatcher != null) {
                useCustomDispatcher(builder.customDispatcher!!)
            }
        }
        return this
    }

    // Note: authorizationServer builder could be added here in a similar way

    fun build(): JsOmniAiConfig {
        return JsOmniAiConfig(dsl.build())
    }
}

@JsExport
@JsName("InboundsBuilder")
class JsInboundsBuilder {
    internal var openAiConnector: InboundConnector<OpenAiChatCompletionsRequest, OpenAiChatCompletionsResponse, OpenAiChatCompletionsResponse>? = null
    internal var anthropicConnector: InboundConnector<AnthropicMessagesRequest, AnthropicMessageResponse, AnthropicStreamEvent>? = null
    internal var geminiConnector: InboundConnector<GeminiGenerateContentRequest, GeminiGenerateContentResponse, GeminiGenerateContentResponse>? = null

    fun openAi(connector: dynamic) {
        this.openAiConnector = connector.unsafeCast<InboundConnector<OpenAiChatCompletionsRequest, OpenAiChatCompletionsResponse, OpenAiChatCompletionsResponse>>()
    }

    fun anthropic(connector: dynamic) {
        this.anthropicConnector = connector.unsafeCast<InboundConnector<AnthropicMessagesRequest, AnthropicMessageResponse, AnthropicStreamEvent>>()
    }

    fun gemini(connector: dynamic) {
        this.geminiConnector = connector.unsafeCast<InboundConnector<GeminiGenerateContentRequest, GeminiGenerateContentResponse, GeminiGenerateContentResponse>>()
    }
}

@JsExport
@JsName("ExecutionBuilder")
class JsExecutionBuilder {
    internal var isNativePipeline = false
    internal val outboundsList = mutableListOf<OutboundPort>()
    internal val interceptorsList = mutableListOf<Interceptor>()
    internal var customDispatcher: DispatcherPort? = null

    fun useNativePipeline(setup: (JsNativePipelineBuilder) -> Unit) {
        isNativePipeline = true
        val builder = JsNativePipelineBuilder()
        setup(builder)
        outboundsList.addAll(builder.outboundsList)
        interceptorsList.addAll(builder.interceptorsList)
    }

    fun useCustomDispatcher(dispatcher: dynamic) {
        this.customDispatcher = dispatcher.unsafeCast<DispatcherPort>()
    }
}

@JsExport
@JsName("NativePipelineBuilder")
class JsNativePipelineBuilder {
    internal val outboundsList = mutableListOf<OutboundPort>()
    internal val interceptorsList = mutableListOf<Interceptor>()

    fun addOutbound(outbound: dynamic) {
        outboundsList.add(outbound.unsafeCast<OutboundPort>())
    }

    fun addInterceptor(interceptor: dynamic) {
        interceptorsList.add(interceptor.unsafeCast<Interceptor>())
    }
}
