package org.omniai.sdk.gateway.client.dsl.interceptors

import org.omniai.sdk.core.commom.AttributeKey
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.metrics.MetricsInterceptor
import org.omniai.sdk.metrics.TelemetryMeter
import org.omniai.sdk.metrics.TelemetryTracer
import org.omniai.sdk.metrics.TracingInterceptor

class TelemetryAttributesBuilder {
    private val keys = mutableListOf<AttributeKey<String>>()

    fun include(key: AttributeKey<String>) {
        keys += key
    }

    internal fun build(): List<AttributeKey<String>> = keys.toList()
}

class TelemetryMetricsInterceptorBuilder {
    var meter: TelemetryMeter? = null
    var tracer: TelemetryTracer? = null
    private var contextTagKeys: List<AttributeKey<String>> = emptyList()

    fun attributes(block: TelemetryAttributesBuilder.() -> Unit) {
        contextTagKeys = TelemetryAttributesBuilder().apply(block).build()
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
