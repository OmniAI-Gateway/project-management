package org.omniai.gateway.interceptors

import org.omniai.sdk.core.http.HttpTransportClient
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.MetricsInterceptor
import org.omniai.sdk.interceptors.auth.*

suspend fun defaultGatewayInterceptors(
    configSource: ConfigSource,
    clientProvider: () -> HttpTransportClient,
    logger: GatewayLogger = NoOpGatewayLogger
): List<Interceptor> {

    val authenticator = loadTokenAuthenticator(configSource, clientProvider)
    return listOf(
        AuthContextInterceptor(authenticator = authenticator),
        RequestLoggingInterceptor(logger),
        MetricsInterceptor()
    )
}
