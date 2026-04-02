package org.omniai.gateway.interceptors

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.InterceptorChain
import org.omniai.sdk.core.pipeline.PipelineResult

class RequestLoggingInterceptor : Interceptor {
    override suspend fun handle(
        context: org.omniai.sdk.core.pipeline.GatewayContext,
        chain: InterceptorChain
    ): PipelineResult {
        println("[gateway] request provider=${context.request.provider.value} model=${context.request.model}")

        return when (val result = chain.proceed(context)) {
            is PipelineResult.Unary -> {
                println("[gateway] unary response provider=${result.response.provider.value} model=${result.response.model}")
                result
            }
            is PipelineResult.Stream -> {
                val traced: Flow<org.omniai.sdk.domain.responses.CommonResponseEvent> = result.eventFlow.onCompletion { cause ->
                    if (cause == null) {
                        println("[gateway] stream completed")
                    } else {
                        println("[gateway] stream failed: ${cause.message}")
                    }
                }
                PipelineResult.Stream(traced)
            }
        }
    }
}

