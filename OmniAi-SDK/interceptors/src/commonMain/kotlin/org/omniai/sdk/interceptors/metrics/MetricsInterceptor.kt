package org.omniai.sdk.interceptors.metrics

import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.omniai.sdk.core.commom.AttributeKey
import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.InterceptorChain
import org.omniai.sdk.core.pipeline.PipelineResult
import kotlin.time.DurationUnit
import kotlin.time.TimeSource.Monotonic.markNow

class MetricsInterceptor(
    private val meter: Meter,
    private val metricsPort: MetricsPort? = null,
    private val config: MetricsInterceptorConfig = MetricsInterceptorConfig()
) : Interceptor {

    private data class InstrumentKey(
        val type: InstrumentType,
        val name: String
    )

    private val customInstruments = mutableMapOf<InstrumentKey, Any>()
    private val customInstrumentsMutex = Mutex()

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
            val durationMs = startedAt.elapsedNow().toDouble(DurationUnit.MILLISECONDS)
            emitMetrics(
                context = context,
                result = null,
                durationMs = durationMs,
                status = STATUS_ERROR,
                errorType = thrown?.let { it::class.simpleName ?: UNKNOWN_EXCEPTION }
            )
            throw thrown ?: IllegalStateException()
        }

        return when (result) {
            is PipelineResult.Stream -> {
                var responseProvider: String? = null
                var responseModel: String? = null

                val wrapped = result.eventFlow
                    .onEach { event ->
                        if (responseProvider == null && responseModel == null) {
                            responseProvider = event.provider.value
                            responseModel = event.model.model
                        }
                    }
                    .onCompletion { cause ->
                        val durationMs = startedAt.elapsedNow().toDouble(DurationUnit.MILLISECONDS)
                        emitMetrics(
                            context = context,
                            result = result,
                            durationMs = durationMs,
                            status = if (cause == null) STATUS_OK else STATUS_ERROR,
                            errorType = cause?.let { it::class.simpleName ?: UNKNOWN_EXCEPTION },
                            streamResponseProvider = responseProvider,
                            streamResponseModel = responseModel
                        )
                    }
                PipelineResult.Stream(wrapped)
            }

            is PipelineResult.Error -> {
                val durationMs = startedAt.elapsedNow().toDouble(DurationUnit.MILLISECONDS)
                emitMetrics(
                    context = context,
                    result = result,
                    durationMs = durationMs,
                    status = STATUS_ERROR,
                    errorType = result.error::class.simpleName ?: DOMAIN_ERROR
                )
                result
            }
            is PipelineResult.Unary,
            is PipelineResult.NoResult -> {
                val durationMs = startedAt.elapsedNow().toDouble(DurationUnit.MILLISECONDS)
                emitMetrics(
                    context = context,
                    result = result,
                    durationMs = durationMs,
                    status = STATUS_OK
                )
                result
            }
        }
    }

    private suspend fun emitMetrics(
        context: GatewayContext,
        result: PipelineResult? = null,
        durationMs: Double,
        status: String,
        errorType: String? = null,
        streamResponseProvider: String? = null,
        streamResponseModel: String? = null
    ) {
        val baseAttributes = buildMetricAttributes(
            context = context,
            result = result,
            status = status,
            errorType = errorType,
            streamResponseProvider = streamResponseProvider,
            streamResponseModel = streamResponseModel
        )

        if (config.defaultLatency.enabled) {
            meter.recordLatency(
                config.defaultLatency.name,
                durationMs,
                baseAttributes
            )
        }

        val port = metricsPort ?: return
        if (config.customMetrics.isEmpty()) return

        for (metric in config.customMetrics) {
            val value = metric.extractor(context, result) ?: continue
            val metricAttributes = mergeWithoutOverride(
                base = baseAttributes,
                extras = metric.attributes(context, result)
            )
            when (metric.type) {
                InstrumentType.COUNTER -> {
                    val instrument = resolveCounter(port, metric)
                    instrument.add(value, metricAttributes)
                }
                InstrumentType.HISTOGRAM -> {
                    val instrument = resolveHistogram(port, metric)
                    instrument.record(value, metricAttributes)
                }
                InstrumentType.UP_DOWN_COUNTER -> {
                    val instrument = resolveUpDownCounter(port, metric)
                    instrument.add(value, metricAttributes)
                }
            }
        }
    }

    private fun buildMetricAttributes(
        context: GatewayContext,
        result: PipelineResult?,
        status: String,
        errorType: String?,
        streamResponseProvider: String?,
        streamResponseModel: String?
    ): Map<String, String> {
        val attrs = mutableMapOf(
            "providerRequest" to context.request.provider.value,
            "modelRequest" to context.request.model,
            "mode" to context.mode.name,
            "status" to status
        )

        if (errorType != null) {
            attrs["error.type"] = errorType
        }

        when (result) {
            is PipelineResult.Unary -> {
                attrs["providerResponse"] = result.response.provider.value
                attrs["modelResponse"] = result.response.model
            }
            else -> Unit
        }

        if (streamResponseProvider != null) {
            attrs["providerResponse"] = streamResponseProvider
        }
        if (streamResponseModel != null) {
            attrs["modelResponse"] = streamResponseModel
        }

        config.defaultLatency.additionalAttributes.forEach { extractor ->
            attrs.putAllWithoutOverride(extractor(context, result))
        }

        config.attributeExtractors.forEach { extractor ->
            attrs.putAllWithoutOverride(extractor(context, result))
        }

        return attrs
    }

    private suspend fun resolveCounter(port: MetricsPort, metric: CustomMetric): CounterMetric {
        return resolveInstrument(InstrumentKey(metric.type, metric.name)) {
            port.counter(metric.name, metric.description)
        } as CounterMetric
    }

    private suspend fun resolveHistogram(port: MetricsPort, metric: CustomMetric): HistogramMetric {
        return resolveInstrument(InstrumentKey(metric.type, metric.name)) {
            port.histogram(metric.name, metric.description)
        } as HistogramMetric
    }

    private suspend fun resolveUpDownCounter(port: MetricsPort, metric: CustomMetric): UpDownCounterMetric {
        return resolveInstrument(InstrumentKey(metric.type, metric.name)) {
            port.upDownCounter(metric.name, metric.description)
        } as UpDownCounterMetric
    }

    private suspend fun resolveInstrument(key: InstrumentKey, create: () -> Any): Any {
        return customInstrumentsMutex.withLock {
            customInstruments.getOrPut(key, create)
        }
    }

    private fun mergeWithoutOverride(
        base: Map<String, String>,
        extras: Map<String, String>
    ): Map<String, String> {
        val merged = base.toMutableMap()
        merged.putAllWithoutOverride(extras)
        return merged
    }

    private fun MutableMap<String, String>.putAllWithoutOverride(values: Map<String, String>) {
        values.forEach { (key, value) ->
            if (key !in this) {
                this[key] = value
            }
        }
    }

    companion object {
        private const val STATUS_OK = "ok"
        private const val STATUS_ERROR = "error"
        private const val UNKNOWN_EXCEPTION = "UnknownException"
        private const val DOMAIN_ERROR = "DomainError"
    }
}
