package org.omniai.sdk.core.pipeline

import kotlin.jvm.JvmInline
import org.omniai.sdk.domain.requests.CommonRequest

@JvmInline
value class AttributeKey<T : Any>(val name: String)

data class GatewayContext(
    val request: CommonRequest,
    val res: PipelineResult,
    private val attributes: MutableMap<String, Any> = mutableMapOf()
) {
    fun <T : Any> get(key: AttributeKey<T>): T? = attributes[key.name] as? T

    fun <T : Any> put(key: AttributeKey<T>, value: T) {
        attributes[key.name] = value
    }

    fun <T : Any> require(key: AttributeKey<T>): T =
        get(key) ?: error("Falta um atributo obrigatório no contexto: ${key.name}")
}

