import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.InterceptorChain
import org.omniai.sdk.core.pipeline.PipelineResult
import org.slf4j.LoggerFactory

class RequestLoggingInterceptor : Interceptor {

    companion object {
        private val logger = LoggerFactory.getLogger(RequestLoggingInterceptor::class.java)
    }

    override suspend fun handle(
        context: org.omniai.sdk.core.pipeline.GatewayContext,
        chain: InterceptorChain
    ): PipelineResult {

        logger.info("[gateway] request provider={} model={}", context.request.provider.value, context.request.model)

        return when (val result = chain.proceed(context)) {
            is PipelineResult.Unary -> {
                logger.info("[gateway] unary response provider={} model={}", result.response.provider.value, result.response.model)
                result
            }
            is PipelineResult.Stream -> {
                val traced: Flow<org.omniai.sdk.domain.responses.CommonResponseEvent> = result.eventFlow.onCompletion { cause ->
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