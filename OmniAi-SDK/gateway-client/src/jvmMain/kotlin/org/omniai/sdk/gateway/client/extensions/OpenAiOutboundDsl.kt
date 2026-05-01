package org.omniai.sdk.gateway.client.extensions

import org.omniai.sdk.core.http.HttpTransportClient
import org.omniai.sdk.core.ports.OutboundPort
import org.omniai.sdk.gateway.client.dsl.outbounds.OutboundsDsl
import org.omniai.sdk.adapters.openai.OpenAiOutboundAdapter

data class OpenAiOutboundConfig(
    val key: String,
    val models: List<String>,
    val baseUrl: String? = null
)

class OpenAiOutboundBuilder(private val httpClient: HttpTransportClient) {
    private val configurations = mutableListOf<OpenAiOutboundConfig>()
    private var currentBaseUrl: String? = null

    fun baseUrl(url: String) {
        currentBaseUrl = url
    }

    fun apiKey(key: String, block: ModelMappingBuilder.() -> Unit) {
        val mapping = ModelMappingBuilder().apply(block).build()
        configurations.add(OpenAiOutboundConfig(key, mapping, currentBaseUrl))
        currentBaseUrl = null
    }

    internal fun buildPorts(): List<OutboundPort> {
        val ports = mutableListOf<OutboundPort>()
        configurations.forEach { config ->
            config.models.forEach { modelName ->
                ports.add(
                    if (config.baseUrl != null) {
                        OpenAiOutboundAdapter(
                            model = org.omniai.sdk.domain.common.Model(modelName),
                            apiKey = config.key,
                            baseUrl = config.baseUrl,
                            transportClient = httpClient
                        )
                    } else {
                        OpenAiOutboundAdapter(
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

class ModelMappingBuilder {
    private val selectedModels = mutableListOf<String>()

    fun models(vararg names: String) {
        selectedModels.addAll(names)
    }

    internal fun build(): List<String> = selectedModels.toList()
}

fun OutboundsDsl.openAI(httpClient: HttpTransportClient, block: OpenAiOutboundBuilder.() -> Unit) {
    val builder = OpenAiOutboundBuilder(httpClient).apply(block)
    builder.buildPorts().forEach { use(it) }
}
