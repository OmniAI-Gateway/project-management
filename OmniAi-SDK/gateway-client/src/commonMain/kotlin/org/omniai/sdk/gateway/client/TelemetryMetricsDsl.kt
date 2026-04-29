package org.omniai.sdk.gateway.client

import org.omniai.sdk.core.commom.AttributeKey
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.metrics.MetricsInterceptor
import org.omniai.sdk.metrics.TelemetryMeter
import org.omniai.sdk.metrics.TelemetryTracer
import org.omniai.sdk.metrics.TracingInterceptor

class TelemetryMetricsInterceptorBuilder {
    var meter: TelemetryMeter? = null
    var tracer: TelemetryTracer? = null
    var contextTagKeys: List<AttributeKey<String>> = emptyList()

    fun tags(vararg keys: AttributeKey<String>) {
        contextTagKeys = keys.toList()
    }

    internal fun build(): List<Interceptor> {
        val resolvedMeter = requireNotNull(meter) {
            "Telemetry meter is required to build telemetry metrics interceptors"
        }

        val interceptors = mutableListOf<Interceptor>()
        tracer?.let { interceptors += TracingInterceptor(it) }
        interceptors += MetricsInterceptor(resolvedMeter, contextTagKeys)
        return interceptors
    }
}

fun telemetryMetricsInterceptorBuild(
    block: TelemetryMetricsInterceptorBuilder.() -> Unit
): List<Interceptor> = TelemetryMetricsInterceptorBuilder().apply(block).build()

