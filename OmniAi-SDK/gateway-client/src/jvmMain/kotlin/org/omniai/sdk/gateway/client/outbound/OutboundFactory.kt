package org.omniai.sdk.gateway.client.outbound

import kotlin.reflect.KClass
import org.omniai.sdk.adapters.anthropic.AnthropicOutboundAdapter
import org.omniai.sdk.adapters.gemini.GeminiOutboundAdapter
import org.omniai.sdk.adapters.openai.OpenAiOutboundAdapter
import org.omniai.sdk.core.ports.OutboundPort
import org.omniai.sdk.domain.common.Model

data class OutboundTarget(
    val outboundClass: KClass<out OutboundPort>,
    val model: String,
    val apiKey: String,
    val baseUrl: String
)

fun buildOutbound(target: OutboundTarget): OutboundPort =
    buildOutbound(
        outboundClass = target.outboundClass,
        model = target.model,
        apiKey = target.apiKey,
        baseUrl = target.baseUrl
    )

fun buildOutbounds(targets: Iterable<OutboundTarget>): List<OutboundPort> =
    targets.map(::buildOutbound)

fun buildOutbound(
    outboundClass: KClass<out OutboundPort>,
    model: String,
    apiKey: String,
    baseUrl: String
): OutboundPort {
    val modelValue = Model(model)
    return when (outboundClass) {
        OpenAiOutboundAdapter::class -> OpenAiOutboundAdapter(
            model = modelValue,
            apiKey = apiKey,
            baseUrl = baseUrl
        )

        GeminiOutboundAdapter::class -> GeminiOutboundAdapter(
            model = modelValue,
            apiKey = apiKey,
            baseUrl = baseUrl
        )

        AnthropicOutboundAdapter::class -> AnthropicOutboundAdapter(
            model = modelValue,
            apiKey = apiKey,
            baseUrl = baseUrl
        )

        else -> error(
            "Unsupported outbound class '${outboundClass.qualifiedName}'. " +
                "Supported built-ins: OpenAiOutboundAdapter, GeminiOutboundAdapter, AnthropicOutboundAdapter."
        )
    }
}

