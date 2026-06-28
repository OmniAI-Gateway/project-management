package org.omniai.sdk.telemetry

import io.opentelemetry.api.OpenTelemetry
import kotlin.test.Test
import kotlin.test.assertSame

class JvmMetricsPortAdapterTest {
    @Test
    fun `counter instruments are reused`() {
        val adapter = JvmMetricsPortAdapter(OpenTelemetry.noop())

        val first = adapter.counter("gateway.requests.total", "Total requests")
        val second = adapter.counter("gateway.requests.total", "Total requests")

        assertSame(first, second)
        first.add(1.0, mapOf("provider" to "openai"))
    }

    @Test
    fun `histogram instruments are reused`() {
        val adapter = JvmMetricsPortAdapter(OpenTelemetry.noop())

        val first = adapter.histogram("gateway.request.duration", "Request duration")
        val second = adapter.histogram("gateway.request.duration", "Request duration")

        assertSame(first, second)
        first.record(32.5, mapOf("mode" to "UNARY"))
    }

    @Test
    fun `updowncounter instruments are reused`() {
        val adapter = JvmMetricsPortAdapter(OpenTelemetry.noop())

        val first = adapter.upDownCounter("gateway.streams.active", "Active streams")
        val second = adapter.upDownCounter("gateway.streams.active", "Active streams")

        assertSame(first, second)
        first.add(1.0, mapOf("provider" to "openai"))
        first.add(-1.0, mapOf("provider" to "openai"))
    }
}
