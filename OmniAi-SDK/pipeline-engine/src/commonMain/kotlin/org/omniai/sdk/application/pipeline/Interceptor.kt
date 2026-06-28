package org.omniai.sdk.application.pipeline

fun interface Interceptor {
    suspend fun handle(
        context: GatewayContext,
        chain: InterceptorChain,
    ): PipelineResult
}

interface InterceptorChain {
    suspend fun proceed(context: GatewayContext): PipelineResult
}
