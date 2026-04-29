package org.omniai.sdk.gateway.client

import org.omniai.sdk.core.ports.InferenceServicePort

class AiServicesDsl {
    private var mode: AiServiceSelection = AiServiceSelection.BuiltIn

    fun builtIn() {
        mode = AiServiceSelection.BuiltIn
    }

    fun custom(service: InferenceServicePort) {
        mode = AiServiceSelection.Custom(service)
    }

    internal fun build(): AiServiceSelection = mode
}

