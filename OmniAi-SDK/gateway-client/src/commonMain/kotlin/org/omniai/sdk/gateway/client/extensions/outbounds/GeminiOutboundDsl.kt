package org.omniai.sdk.gateway.client.extensions.outbounds

import org.omniai.sdk.adapters.gemini.GeminiOutboundAdapter
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.gateway.client.dsl.outbounds.OutboundsDsl
import org.omniai.sdk.ports.outbound.OutboundPort
import org.omniai.sdk.ports.outbound.http.HttpTransportClient

data class GeminiOutboundConfig(
    val key: String,
    val models: List<String>,
    val baseUrl: String? = null,
)

class GeminiOutboundBuilder(
    private val httpClient: HttpTransportClient,
) {
    private val configurations = mutableListOf<GeminiOutboundConfig>()
    private var currentBaseUrl: String? = null

    fun baseUrl(url: String) {
        currentBaseUrl = url
    }

    fun apiKey(
        key: String,
        block: ModelMappingBuilder.() -> Unit,
    ) {
        val mapping = ModelMappingBuilder().apply(block).build()
        configurations.add(GeminiOutboundConfig(key, mapping, currentBaseUrl))
        currentBaseUrl = null
    }

    internal fun buildPorts(): List<OutboundPort> {
        val ports = mutableListOf<OutboundPort>()
        configurations.forEach { config ->
            config.models.forEach { modelName ->
                ports.add(
                    if (config.baseUrl != null) {
                        GeminiOutboundAdapter(
                            model = Model(modelName),
                            apiKey = config.key,
                            baseUrl = config.baseUrl,
                            transportClient = httpClient,
                        )
                    } else {
                        GeminiOutboundAdapter(
                            model = Model(modelName),
                            apiKey = config.key,
                            transportClient = httpClient,
                        )
                    },
                )
            }
        }
        return ports
    }
}

fun OutboundsDsl.gemini(
    httpClient: HttpTransportClient,
    block: GeminiOutboundBuilder.() -> Unit,
) {
    val builder = GeminiOutboundBuilder(httpClient).apply(block)
    builder.buildPorts().forEach { use(it) }
}
