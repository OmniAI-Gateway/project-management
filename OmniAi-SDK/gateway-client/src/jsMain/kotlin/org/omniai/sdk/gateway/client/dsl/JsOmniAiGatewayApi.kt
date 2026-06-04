package org.omniai.sdk.gateway.client.dsl

import org.omniai.sdk.application.pipeline.Interceptor
import org.omniai.sdk.ports.inbound.InboundConnector
import org.omniai.sdk.ports.inbound.DispatcherPort
import org.omniai.sdk.ports.outbound.OutboundPort
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.output.AnthropicMessageResponse
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent
import org.omniai.sdk.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniai.sdk.contracts.gemini.output.GeminiGenerateContentResponse
import org.omniai.sdk.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniai.sdk.contracts.openai.output.OpenAiChatCompletionsResponse
import org.omniai.sdk.gateway.client.core.OmniAiConfig
import org.omniai.sdk.gateway.client.extensions.inbounds.openAi
import org.omniai.sdk.gateway.client.extensions.inbounds.anthropic
import org.omniai.sdk.gateway.client.extensions.inbounds.gemini
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
fun jsOmniAiGateway(setup: (JsOmniAiGatewayBuilder) -> Unit): JsOmniAiConfig {
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
            builder.setups.forEach { it(this) }
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
    internal val setups = mutableListOf<(org.omniai.sdk.gateway.client.dsl.inbounds.InboundsDsl) -> Unit>()

    fun openAi(connector: dynamic) {
        val typedConnector = connector.unsafeCast<InboundConnector<OpenAiChatCompletionsRequest, OpenAiChatCompletionsResponse, OpenAiChatCompletionsResponse>>()
        setups.add { dsl -> dsl.openAi(typedConnector) }
    }

    fun anthropic(connector: dynamic) {
        val typedConnector = connector.unsafeCast<InboundConnector<AnthropicMessagesRequest, AnthropicMessageResponse, AnthropicStreamEvent>>()
        setups.add { dsl -> dsl.anthropic(typedConnector) }
    }

    fun gemini(connector: dynamic) {
        val typedConnector = connector.unsafeCast<InboundConnector<GeminiGenerateContentRequest, GeminiGenerateContentResponse, GeminiGenerateContentResponse>>()
        setups.add { dsl -> dsl.gemini(typedConnector) }
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
