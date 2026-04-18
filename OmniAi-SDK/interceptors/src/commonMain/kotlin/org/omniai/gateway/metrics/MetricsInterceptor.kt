package org.omniai.gateway.metrics

import kotlinx.coroutines.flow.onCompletion
import org.omniai.sdk.core.commom.AttributeKey
import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.InterceptorChain
import org.omniai.sdk.core.pipeline.PipelineResult
import kotlin.time.DurationUnit
import kotlin.time.TimeSource.Monotonic.markNow


class MetricsInterceptor(
    private val meter: TelemetryMeter,
    private val contextTagKeys: List<AttributeKey<String>> = emptyList()
) : Interceptor {

    override suspend fun handle(context: GatewayContext, chain: InterceptorChain): PipelineResult {
        val startedAt = markNow()
        var thrown: Throwable? = null

        val result = try {
            chain.proceed(context)
        } catch (t: Throwable) {
            thrown = t
            null
        }

        if (result == null) {
            val attrs = buildAttrs(context).toMutableMap().apply {
                this["error.type"] = thrown?.let { it::class.simpleName ?: "UnknownException" } ?: "UnknownException"
            }
            meter.recordLatency(
                METRICS_NAME,
                startedAt.elapsedNow().toDouble(DurationUnit.MILLISECONDS),
                attrs
            )
            throw thrown ?: IllegalStateException()
        }

        return when (result) {
            is PipelineResult.Stream -> {
                val wrapped = result.eventFlow.onCompletion { cause ->
                    val attrs = buildAttrs(context).toMutableMap()
                    if (cause != null) {
                        attrs["error.type"] = cause::class.simpleName ?: "UnknownException"
                    }
                    meter.recordLatency(
                        METRICS_NAME,
                        startedAt.elapsedNow().toDouble(DurationUnit.MILLISECONDS),
                        attrs
                    )
                }
                PipelineResult.Stream(wrapped)
            }

            is PipelineResult.Error -> {
                val attrs = buildAttrs(context).toMutableMap().apply {
                    this["error.type"] = result.error::class.simpleName ?: "DomainError"
                }
                meter.recordLatency(
                    METRICS_NAME,
                    startedAt.elapsedNow().inWholeMilliseconds.toDouble(),
                    attrs
                )
                result
            }
            is PipelineResult.Unary,
            is PipelineResult.NoResult -> {
                meter.recordLatency(
                    METRICS_NAME,
                    startedAt.elapsedNow().toDouble(DurationUnit.MILLISECONDS),
                    buildAttrs(context)
                )
                result
            }
        }
    }

    private fun buildAttrs(context: GatewayContext): Map<String, String> {
        val attrs = mutableMapOf(
            "provider" to context.request.provider.value,
            "model" to context.request.model,
            "mode" to context.mode.name
        )
        contextTagKeys.forEach { key ->
            context.attributes[key]?.let { attrs[key.name] = it }
        }
        return attrs
    }

    companion object {
        private const val METRICS_NAME = "gateway.inference.request.duration"
    }
}
