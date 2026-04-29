package org.omniai.sdk.logging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.InterceptorChain
import org.omniai.sdk.core.pipeline.PipelineResult
import org.omniai.sdk.domain.responses.CommonResponseEvent
import org.omniai.sdk.logger.*

class RequestLoggingInterceptor(
    private val logger: GatewayLogger = NoOpGatewayLogger
) : Interceptor {

    override suspend fun handle(
        context: GatewayContext,
        chain: InterceptorChain
    ): PipelineResult {
        logger.info("[gateway] request provider={} model={}", context.request.provider.value, context.request.model)

        return when (val result = chain.proceed(context)) {
            is PipelineResult.Unary -> {
                logger.info("[gateway] unary response provider={} model={}", result.response.provider.value, result.response.model)
                result
            }

            is PipelineResult.Stream -> {
                val traced: Flow<CommonResponseEvent> = result.eventFlow.onCompletion { cause ->
                    if (cause == null) {
                        logger.info("[gateway] stream completed")
                    } else {
                        logger.error("[gateway] stream failed: {}", cause.message, cause)
                    }
                }
                PipelineResult.Stream(traced)
            }

            is PipelineResult.Error -> {
                logger.warn("[gateway] request failed with domain error: {}", result.error.message)
                result
            }

            is PipelineResult.NoResult -> PipelineResult.NoResult
        }
    }
}

