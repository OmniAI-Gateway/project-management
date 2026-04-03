package org.omniai.gateway.interceptors

import RequestLoggingInterceptor
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.MetricsInterceptor

fun defaultGatewayInterceptors(): List<Interceptor> = listOf(
    RequestLoggingInterceptor(),
    MetricsInterceptor()
)

