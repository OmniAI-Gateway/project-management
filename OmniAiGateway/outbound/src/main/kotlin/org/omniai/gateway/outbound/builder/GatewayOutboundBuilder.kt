package org.omniai.gateway.outbound.builder

import kotlin.reflect.KClass
import org.omniai.sdk.adapters.anthropic.AnthropicOutboundAdapter
import org.omniai.sdk.adapters.gemini.GeminiOutboundAdapter
import org.omniai.sdk.adapters.openai.OpenAiOutboundAdapter
import org.omniai.sdk.core.ports.OutboundPort
import org.omniai.sdk.domain.common.Model

/**
 * Dynamic outbound constructor fed by adapter KClass and model/baseUrl/apiKey targets.
 */
data class OutboundTargetConfig(
    val model: String,
    val baseUrl: String,
    val apiKey: String
)

data class OutboundBuildInput(
    val outboundClass: KClass<out OutboundPort>,
    val targets: List<OutboundTargetConfig>
)

class GatewayOutboundBuilder {
    fun build(inputs: List<OutboundBuildInput>): List<OutboundPort> =
        inputs.flatMap { input ->
            input.targets.map { target -> instantiate(input.outboundClass, target) }
        }

    fun fromModelUrlPairs(
        outboundClass: KClass<out OutboundPort>,
        apiKey: String,
        modelUrlPairs: List<Pair<String, String>>
    ): OutboundBuildInput = OutboundBuildInput(
        outboundClass = outboundClass,
        targets = modelUrlPairs.map { (model, url) ->
            OutboundTargetConfig(model = model, baseUrl = url, apiKey = apiKey)
        }
    )

    private fun instantiate(
        outboundClass: KClass<out OutboundPort>,
        target: OutboundTargetConfig
    ): OutboundPort = when (outboundClass) {
        OpenAiOutboundAdapter::class -> OpenAiOutboundAdapter(
            model = Model(target.model),
            apiKey = target.apiKey,
            baseUrl = target.baseUrl
        )
        GeminiOutboundAdapter::class -> GeminiOutboundAdapter(
            model = Model(target.model),
            apiKey = target.apiKey,
            baseUrl = target.baseUrl
        )
        AnthropicOutboundAdapter::class -> AnthropicOutboundAdapter(
            model = Model(target.model),
            apiKey = target.apiKey,
            baseUrl = target.baseUrl
        )
        else -> error("Unsupported outbound class: ${outboundClass.qualifiedName}")
    }
}

