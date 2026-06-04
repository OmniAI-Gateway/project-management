package org.omniai.sdk.application.pipeline

import org.omniai.sdk.ports.inbound.DispatcherPort

class GatewayPipelineBuilder {
    private val interceptors = mutableListOf<Interceptor>()
    private lateinit var terminalDispatcher: DispatcherPort

    fun install(interceptor: Interceptor) {
        interceptors.add(interceptor)
    }

    fun installDispatcher(dispatcherPort: DispatcherPort) {
        terminalDispatcher = dispatcherPort
    }

    fun intercept(block: suspend (GatewayContext, InterceptorChain) -> PipelineResult) {
        interceptors.add(Interceptor { ctx, ch -> block(ctx, ch) })
    }

    fun provider(interceptor: Interceptor) {
        interceptors.add(interceptor)
    }

    fun build(): GatewayPipeline {
        check(::terminalDispatcher.isInitialized) {
            "Terminal dispatcher obrigatório: chama installDispatcher(...) antes de build()."
        }
        return GatewayPipeline(interceptors.toList(), terminalDispatcher)
    }
}

