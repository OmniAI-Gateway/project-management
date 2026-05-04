package org.omniai.sdk.gateway.client.dsl.interceptors

import org.omniai.sdk.core.pipeline.Interceptor

class InterceptorsDsl {
    private val interceptors = mutableListOf<Interceptor>()

    fun use(interceptor: Interceptor) {
        interceptors += interceptor
    }

    /**
     * Installs telemetry metrics interceptors based on configuration.
     */
    fun metrics(block: MetricsInterceptorBuilder.() -> Unit) {
        metricsInterceptorBuild(block).forEach(::use)
    }

    /**
     * Configures and installs a RateLimitInterceptor.
     */
    fun rateLimiting(block: RateLimitingInterceptorBuilder.() -> Unit) {
        use(RateLimitingInterceptorBuilder().apply(block).build())
    }

    internal fun build(): List<Interceptor> = interceptors.toList()
}
