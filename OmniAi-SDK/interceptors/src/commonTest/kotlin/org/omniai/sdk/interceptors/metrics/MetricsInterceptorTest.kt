package org.omniai.sdk.interceptors.metrics

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail
import org.omniai.sdk.core.commom.key
import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.core.pipeline.InterceptorChain
import org.omniai.sdk.core.pipeline.PipelineResult
import org.omniai.sdk.domain.common.CommonRole
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.requests.CommonRequestMessage
import org.omniai.sdk.domain.responses.CommonChoice
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseMessage
import org.omniai.sdk.domain.responses.ResponseStarted

class MetricsInterceptorTest {

    @Test
    fun `default histogram and typed attributes are emitted for unary responses`() = runTest {
        val meter = RecordingMeter()
        val metricsPort = RecordingMetricsPort()

        val tenantKey = key<String>("tenant.id")
        val statusCodeKey = key<Int>("http.statusCode")
        val betaKey = key<Boolean>("feature.beta")

        val context = baseContext()
        context.attributes[tenantKey] = "acme"
        context.attributes[statusCodeKey] = 200
        context.attributes[betaKey] = true
        context.attributes.put("token.total", 42)

        val interceptor = MetricsInterceptor(
            meter = meter,
            metricsPort = metricsPort,
            config = MetricsInterceptorConfig(
                attributeExtractors = MetricsAttributesBuilder().apply {
                    include(tenantKey)
                    include(statusCodeKey, alias = "http.status_code")
                    include(betaKey)
                }.build(),
                customMetrics = metricsConfiguration {
                    counter("gateway.requests.total", "Total requests") {
                        value { _, _ -> 1 }
                        tags { _, _ -> mapOf("kind" to "all") }
                    }
                    histogram("gateway.tokens.total", "Total tokens") {
                        value { ctx, _ -> ctx.attributes.get<Int>("token.total") }
                    }
                }
            )
        )

        val result = interceptor.handle(context, StaticChain { PipelineResult.Unary(unaryResponse()) })
        assertIs<PipelineResult.Unary>(result)

        assertEquals(1, meter.records.size)
        val defaultAttrs = meter.records.single().attributes
        assertEquals("openai", defaultAttrs["providerRequest"])
        assertEquals("gpt-4o-mini", defaultAttrs["modelRequest"])
        assertEquals("UNARY", defaultAttrs["mode"])
        assertEquals("ok", defaultAttrs["status"])
        assertEquals("openai", defaultAttrs["providerResponse"])
        assertEquals("gpt-4o-mini", defaultAttrs["modelResponse"])
        assertEquals("acme", defaultAttrs["tenant.id"])
        assertEquals("200", defaultAttrs["http.status_code"])
        assertEquals("true", defaultAttrs["feature.beta"])

        assertEquals(2, metricsPort.emissions.size)
        val counterMetric = metricsPort.emissions.first { it.name == "gateway.requests.total" }
        assertEquals(1.0, counterMetric.value)
        assertEquals("all", counterMetric.attributes["kind"])
        assertEquals("acme", counterMetric.attributes["tenant.id"])
    }

    @Test
    fun `stream failures still record default latency metric with error tags`() = runTest {
        val meter = RecordingMeter()
        val interceptor = MetricsInterceptor(meter = meter)
        val context = baseContext()

        val result = interceptor.handle(
            context,
            StaticChain {
                PipelineResult.Stream(
                    flow {
                        emit(
                            ResponseStarted(
                                provider = Provider.OPENAI,
                                id = "resp-stream",
                                model = Model("gpt-4o-mini"),
                                sequence = 1
                            )
                        )
                        throw IllegalStateException("boom")
                    }
                )
            }
        )

        val stream = assertIs<PipelineResult.Stream>(result)
        try {
            stream.eventFlow.collect { }
            fail("Expected stream collection to throw")
        } catch (_: IllegalStateException) {
            // expected
        }

        assertEquals(1, meter.records.size)
        val attrs = meter.records.single().attributes
        assertEquals("error", attrs["status"])
        assertEquals("IllegalStateException", attrs["error.type"])
        assertEquals("openai", attrs["providerResponse"])
    }

    @Test
    fun `custom metric instruments are created once and reused across requests`() = runTest {
        val meter = RecordingMeter()
        val metricsPort = RecordingMetricsPort()
        val interceptor = MetricsInterceptor(
            meter = meter,
            metricsPort = metricsPort,
            config = MetricsInterceptorConfig(
                customMetrics = metricsConfiguration {
                    counter("gateway.requests.total", "Total requests") {
                        value { _, _ -> 1 }
                    }
                }
            )
        )

        val first = interceptor.handle(baseContext(), StaticChain { PipelineResult.Unary(unaryResponse()) })
        val second = interceptor.handle(baseContext(), StaticChain { PipelineResult.Unary(unaryResponse()) })

        assertIs<PipelineResult.Unary>(first)
        assertIs<PipelineResult.Unary>(second)
        assertEquals(1, metricsPort.counterCreations["gateway.requests.total"])
        assertEquals(2, metricsPort.emissions.count { it.name == "gateway.requests.total" })
    }

    @Test
    fun `default latency metric can be disabled`() = runTest {
        val meter = RecordingMeter()
        val interceptor = MetricsInterceptor(
            meter = meter,
            config = MetricsInterceptorConfig(
                defaultLatency = DefaultLatencyMetricConfig(enabled = false)
            )
        )

        val result = interceptor.handle(baseContext(), StaticChain { PipelineResult.Unary(unaryResponse()) })

        assertIs<PipelineResult.Unary>(result)
        assertTrue(meter.records.isEmpty())
    }

    @Test
    fun `default additional attributes cannot override protected default keys`() = runTest {
        val meter = RecordingMeter()
        val interceptor = MetricsInterceptor(
            meter = meter,
            config = MetricsInterceptorConfig(
                defaultLatency = DefaultLatencyMetricConfig(
                    additionalAttributes = MetricsAttributesBuilder().apply {
                        attribute("providerRequest") { _, _ -> "malicious-provider" }
                        attribute("modelRequest") { _, _ -> "malicious-model" }
                        attribute("custom.default.tag") { _, _ -> "ok" }
                    }.build()
                )
            )
        )

        val result = interceptor.handle(baseContext(), StaticChain { PipelineResult.Unary(unaryResponse()) })
        assertIs<PipelineResult.Unary>(result)

        val attributes = meter.records.single().attributes
        assertEquals("openai", attributes["providerRequest"])
        assertEquals("gpt-4o-mini", attributes["modelRequest"])
        assertEquals("ok", attributes["custom.default.tag"])
    }
}

private class StaticChain(
    private val resolver: suspend (GatewayContext) -> PipelineResult
) : InterceptorChain {
    override suspend fun proceed(context: GatewayContext): PipelineResult = resolver(context)
}

private class RecordingMeter : Meter {
    data class Record(
        val metricName: String,
        val durationMs: Double,
        val attributes: Map<String, String>
    )

    val records = mutableListOf<Record>()

    override fun recordLatency(metricName: String, durationMs: Double, attributes: Map<String, String>) {
        records += Record(metricName, durationMs, attributes)
    }
}

private class RecordingMetricsPort : MetricsPort {
    data class Emission(
        val type: InstrumentType,
        val name: String,
        val value: Double,
        val attributes: Map<String, String>
    )

    val counterCreations = mutableMapOf<String, Int>()
    val histogramCreations = mutableMapOf<String, Int>()
    val upDownCreations = mutableMapOf<String, Int>()
    val emissions = mutableListOf<Emission>()

    override fun counter(name: String, description: String): CounterMetric {
        counterCreations[name] = (counterCreations[name] ?: 0) + 1
        return CounterMetric { value, attributes ->
            emissions += Emission(InstrumentType.COUNTER, name, value, attributes)
        }
    }

    override fun histogram(name: String, description: String): HistogramMetric {
        histogramCreations[name] = (histogramCreations[name] ?: 0) + 1
        return HistogramMetric { value, attributes ->
            emissions += Emission(InstrumentType.HISTOGRAM, name, value, attributes)
        }
    }

    override fun upDownCounter(name: String, description: String): UpDownCounterMetric {
        upDownCreations[name] = (upDownCreations[name] ?: 0) + 1
        return UpDownCounterMetric { delta, attributes ->
            emissions += Emission(InstrumentType.UP_DOWN_COUNTER, name, delta, attributes)
        }
    }
}

private fun baseContext(): GatewayContext = GatewayContext(
    request = CommonRequest(
        provider = Provider.OPENAI,
        model = "gpt-4o-mini",
        messages = listOf(
            CommonRequestMessage(
                role = CommonRole.USER,
                content = emptyList()
            )
        )
    )
)

private fun unaryResponse(): CommonResponse = CommonResponse(
    provider = Provider.OPENAI,
    id = "resp-1",
    model = "gpt-4o-mini",
    choices = listOf(
        CommonChoice(
            index = 0,
            message = CommonResponseMessage(
                role = CommonRole.ASSISTANT,
                content = emptyList()
            )
        )
    )
)
