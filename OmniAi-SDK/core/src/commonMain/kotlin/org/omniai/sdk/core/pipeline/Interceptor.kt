package org.omniai.sdk.core.pipeline

fun interface Interceptor {
    suspend fun handle(context: GatewayContext, chain: InterceptorChain): PipelineResult
}

interface InterceptorChain {
    suspend fun proceed(context: GatewayContext): PipelineResult
}

