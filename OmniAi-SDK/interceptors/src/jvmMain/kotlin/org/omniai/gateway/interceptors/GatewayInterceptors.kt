package org.omniai.gateway.interceptors

import org.omniai.sdk.core.pipeline.Interceptor

fun defaultGatewayInterceptors(): List<Interceptor> = defaultGatewayInterceptors(
    configSource = JvmEnvConfigSource,
    logger = Slf4jGatewayLogger(RequestLoggingInterceptor::class.java)
)

