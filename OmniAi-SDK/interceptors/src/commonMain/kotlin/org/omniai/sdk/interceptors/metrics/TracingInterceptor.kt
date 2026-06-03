package org.omniai.sdk.interceptors.metrics

import org.omniai.sdk.application.pipeline.GatewayContext
import org.omniai.sdk.application.pipeline.Interceptor
import org.omniai.sdk.application.pipeline.InterceptorChain
import org.omniai.sdk.application.pipeline.PipelineResult

class TracingInterceptor(private val tracer: Tracer) : Interceptor {
    override suspend fun handle(context: GatewayContext, chain: InterceptorChain): PipelineResult {
        return tracer.withSpan("gateway.request.process") {
            chain.proceed(context)
        }
    }
}
