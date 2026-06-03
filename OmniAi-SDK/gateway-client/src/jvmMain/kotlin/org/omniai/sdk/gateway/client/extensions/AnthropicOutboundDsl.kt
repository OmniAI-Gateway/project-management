package org.omniai.sdk.gateway.client.extensions

import org.omniai.sdk.ports.outbound.http.HttpTransportClient
import org.omniai.sdk.ports.outbound.OutboundPort
import org.omniai.sdk.gateway.client.dsl.outbounds.OutboundsDsl
import org.omniai.sdk.adapters.anthropic.AnthropicOutboundAdapter

data class AnthropicOutboundConfig(
    val key: String,
    val models: List<String>,
    val baseUrl: String? = null
)

class AnthropicOutboundBuilder(private val httpClient: HttpTransportClient) {
    private val configurations = mutableListOf<AnthropicOutboundConfig>()
    private var currentBaseUrl: String? = null

    fun baseUrl(url: String) {
        currentBaseUrl = url
    }

    fun apiKey(key: String, block: ModelMappingBuilder.() -> Unit) {
        val mapping = ModelMappingBuilder().apply(block).build()
        configurations.add(AnthropicOutboundConfig(key, mapping, currentBaseUrl))
        currentBaseUrl = null
    }

    internal fun buildPorts(): List<OutboundPort> {
        val ports = mutableListOf<OutboundPort>()
        configurations.forEach { config ->
            config.models.forEach { modelName ->
                ports.add(
                    if (config.baseUrl != null) {
                        AnthropicOutboundAdapter(
                            model = org.omniai.sdk.domain.common.Model(modelName),
                            apiKey = config.key,
                            baseUrl = config.baseUrl,
                            transportClient = httpClient
                        )
                    } else {
                        AnthropicOutboundAdapter(
                            model = org.omniai.sdk.domain.common.Model(modelName),
                            apiKey = config.key,
                            transportClient = httpClient
                        )
                    }
                )
            }
        }
        return ports
    }
}

fun OutboundsDsl.anthropic(httpClient: HttpTransportClient, block: AnthropicOutboundBuilder.() -> Unit) {
    val builder = AnthropicOutboundBuilder(httpClient).apply(block)
    builder.buildPorts().forEach { use(it) }
}
