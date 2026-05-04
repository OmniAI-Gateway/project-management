import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import org.omniai.sdk.core.commom.key
import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.InterceptorChain
import org.omniai.sdk.core.pipeline.PipelineResult
import org.omniai.sdk.interceptors.metrics.CounterMetric
import org.omniai.sdk.interceptors.metrics.HistogramMetric
import org.omniai.sdk.interceptors.metrics.InstrumentType
import org.omniai.sdk.interceptors.metrics.MetricsInterceptorConfig
import org.omniai.sdk.interceptors.metrics.MetricsPort
import org.omniai.sdk.interceptors.metrics.UpDownCounterMetric
import kotlin.time.DurationUnit
import kotlin.time.TimeMark
import kotlin.time.TimeSource.Monotonic.markNow

class MetricsInterceptor(
    private val metricsPort: MetricsPort,
    private val config: MetricsInterceptorConfig = MetricsInterceptorConfig()
) : Interceptor {

    private val defaultLatencyInstrument = if (config.defaultLatency.enabled) {
        metricsPort.histogram(config.defaultLatency.name, "Gateway request latency", "ms")
    } else null

    private val resolvedCustomMetrics = config.customMetrics.map { metric ->
        val instrument = when (metric.type) {
            InstrumentType.COUNTER -> metricsPort.counter(metric.name, metric.description, metric.unit)
            InstrumentType.HISTOGRAM -> metricsPort.histogram(metric.name, metric.description, metric.unit)
            InstrumentType.UP_DOWN_COUNTER -> metricsPort.upDownCounter(metric.name, metric.description, metric.unit)
        }
        metric to instrument
    }

    override suspend fun handle(context: GatewayContext, chain: InterceptorChain): PipelineResult {
        context.attributes[ATTR_METRICS_START_TIME] = markNow()

        var thrown: Throwable? = null
        val result = try {
            chain.proceed(context)
        } catch (t: Throwable) {
            thrown = t
            null
        }

        if (result == null) {
            finalizeContextState(context, STATUS_ERROR, thrown?.let { it::class.simpleName ?: UNKNOWN_EXCEPTION })
            emitMetrics(context, null)
            throw thrown ?: IllegalStateException()
        }

        return when (result) {
            is PipelineResult.Stream -> {
                val wrapped = result.eventFlow
                    .onEach { event ->
                        if (context.attributes[ATTR_STREAM_RES_PROVIDER] == null) {
                            context.attributes[ATTR_STREAM_RES_PROVIDER] = event.provider.value
                            context.attributes[ATTR_STREAM_RES_MODEL] = event.model.model
                        }
                    }
                    .onCompletion { cause ->
                        finalizeContextState(
                            context,
                            status = if (cause == null) STATUS_OK else STATUS_ERROR,
                            errorType = cause?.let { it::class.simpleName ?: UNKNOWN_EXCEPTION }
                        )
                        emitMetrics(context, result)
                    }
                PipelineResult.Stream(wrapped)
            }

            is PipelineResult.Error -> {
                finalizeContextState(context, STATUS_ERROR, result.error::class.simpleName ?: DOMAIN_ERROR)
                emitMetrics(context, result)
                result
            }
            is PipelineResult.Unary,
            is PipelineResult.NoResult -> {
                finalizeContextState(context, STATUS_OK)
                emitMetrics(context, result)
                result
            }
        }
    }

    private fun finalizeContextState(context: GatewayContext, status: String, errorType: String? = null) {
        val startedAt = context.attributes[ATTR_METRICS_START_TIME] ?: return

        context.attributes[ATTR_METRICS_DURATION_MS] = startedAt.elapsedNow().toDouble(DurationUnit.MILLISECONDS)
        context.attributes[ATTR_METRICS_STATUS] = status

        errorType?.let { context.attributes[ATTR_METRICS_ERROR_TYPE] = it }
    }

    private fun emitMetrics(context: GatewayContext, result: PipelineResult?) {
        val globalAttributes = buildGlobalCustomAttributes(context, result)
        if (defaultLatencyInstrument != null) {
            val durationMs = context.attributes[ATTR_METRICS_DURATION_MS] ?: 0.0
            val defaultLatencyAttrs = buildDefaultMetricsAttributes(context, result, globalAttributes)
            defaultLatencyInstrument.record(durationMs, defaultLatencyAttrs)
        }
        for ((metricConfig, instrument) in resolvedCustomMetrics) {
            val value = metricConfig.extractor(context, result) ?: continue

            val specificTags = metricConfig.attributes(context, result)
            val finalAttributes = specificTags + globalAttributes

            when (instrument) {
                is CounterMetric -> instrument.add(value, finalAttributes)
                is HistogramMetric -> instrument.record(value, finalAttributes)
                is UpDownCounterMetric -> instrument.add(value, finalAttributes)
            }
        }
    }

    private fun buildGlobalCustomAttributes(context: GatewayContext, result: PipelineResult?): Map<String, String> {
        val attrs = mutableMapOf<String, String>()
        config.attributeExtractors.forEach { extractor ->
            val extracted = extractor(context, result)
            extracted.forEach { (k, v) -> if (k !in attrs) attrs[k] = v }
        }
        return attrs
    }

    private fun buildDefaultMetricsAttributes(
        context: GatewayContext,
        result: PipelineResult?,
        globalAttributes: Map<String, String>
    ): Map<String, String> {
        val attrs = mutableMapOf<String, String>()
        attrs["providerRequest"] = context.request.provider.value
        attrs["modelRequest"] = context.request.model
        attrs["mode"] = context.mode.name
        attrs["status"] = context.attributes[ATTR_METRICS_STATUS] ?: "unknown"
        context.attributes[ATTR_METRICS_ERROR_TYPE]?.let { attrs["error.type"] = it }
        if (result is PipelineResult.Unary) {
            attrs["providerResponse"] = result.response.provider.value
            attrs["modelResponse"] = result.response.model
        } else {
            context.attributes[ATTR_STREAM_RES_PROVIDER]?.let { attrs["providerResponse"] = it }
            context.attributes[ATTR_STREAM_RES_MODEL]?.let { attrs["modelResponse"] = it }
        }

        config.defaultLatency.additionalAttributes.forEach { extractor ->
            val extracted = extractor(context, result)
            extracted.forEach { (k, v) -> if (k !in attrs) attrs[k] = v }
        }
        return attrs + globalAttributes
    }

    companion object {
        private const val STATUS_OK = "ok"
        private const val STATUS_ERROR = "error"
        private const val UNKNOWN_EXCEPTION = "UnknownException"
        private const val DOMAIN_ERROR = "DomainError"

        val ATTR_METRICS_START_TIME = key<TimeMark>("metrics.started_at")
        val ATTR_METRICS_DURATION_MS = key<Double>("metrics.duration_ms")
        val ATTR_METRICS_STATUS = key<String>("metrics.status")
        val ATTR_METRICS_ERROR_TYPE = key<String>("metrics.error_type")
        val ATTR_STREAM_RES_PROVIDER = key<String>("metrics.stream_provider")
        val ATTR_STREAM_RES_MODEL = key<String>("metrics.stream_model")
    }
}