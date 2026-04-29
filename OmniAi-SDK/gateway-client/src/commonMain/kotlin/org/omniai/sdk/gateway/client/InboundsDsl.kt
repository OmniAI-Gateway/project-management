package org.omniai.sdk.gateway.client

import org.omniai.sdk.core.ports.InferenceServicePort

class InboundsDsl {
    var openAi: Boolean = true
    var anthropic: Boolean = true
    var gemini: Boolean = true

    private val factories = mutableMapOf<String, (InferenceServicePort) -> Any>()

    fun custom(name: String, factory: (InferenceServicePort) -> Any) {
        factories[name] = factory
    }

    internal fun build(): InboundRegistration = InboundRegistration(
        installOpenAi = openAi,
        installAnthropic = anthropic,
        installGemini = gemini,
        customFactories = factories.toMap()
    )
}

