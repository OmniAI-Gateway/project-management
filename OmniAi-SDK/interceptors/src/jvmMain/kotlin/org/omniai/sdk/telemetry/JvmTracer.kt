package org.omniai.sdk.telemetry

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext
import org.omniai.sdk.interceptors.metrics.Tracer

class JvmTracer(
    openTelemetry: OpenTelemetry,
    instrumentationScopeName: String = "omniai-gateway-sdk",
) : Tracer {
    private val tracer = openTelemetry.getTracer(instrumentationScopeName)

    override suspend fun <T> withSpan(
        spanName: String,
        block: suspend () -> T,
    ): T {
        val span = tracer.spanBuilder(spanName).startSpan()

        return try {
            withContext(span.asContextElement()) {
                block()
            }
        } catch (e: Throwable) {
            span.recordException(e)
            span.setStatus(StatusCode.ERROR, e.message ?: "Unknown Error")
            throw e
        } finally {
            span.end()
        }
    }
}
