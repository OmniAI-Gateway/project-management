package org.omniai.sdk.interceptors.metrics

import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.InterceptorChain
import org.omniai.sdk.core.pipeline.PipelineResult

class TracingInterceptor(private val tracer: TelemetryTracer) : Interceptor {
    override suspend fun handle(context: GatewayContext, chain: InterceptorChain): PipelineResult {
        return tracer.withSpan("gateway.request.process") {
            chain.proceed(context)
        }
    }
}