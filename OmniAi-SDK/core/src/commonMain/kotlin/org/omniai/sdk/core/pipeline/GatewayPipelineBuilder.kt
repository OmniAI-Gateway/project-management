package org.omniai.sdk.core.pipeline

import org.omniai.sdk.core.ports.InferenceServicePort

class GatewayPipelineBuilder {
    private val interceptors = mutableListOf<Interceptor>()
    private lateinit var terminalService: InferenceServicePort

    fun install(interceptor: Interceptor) {
        interceptors.add(interceptor)
    }

    fun installService(servicePort: InferenceServicePort) {
        terminalService = servicePort
    }

    fun intercept(block: suspend (GatewayContext, InterceptorChain) -> PipelineResult) {
        interceptors.add(Interceptor { ctx, ch -> block(ctx, ch) })
    }

    fun provider(interceptor: Interceptor) {
        interceptors.add(interceptor)
    }

    fun build(): GatewayPipeline {
        check(::terminalService.isInitialized) {
            "Terminal service obrigatório: chama installService(...) antes de build()."
        }
        return GatewayPipeline(interceptors.toList(), terminalService)
    }
}

