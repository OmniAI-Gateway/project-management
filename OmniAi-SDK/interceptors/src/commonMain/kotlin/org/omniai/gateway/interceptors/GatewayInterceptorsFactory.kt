package org.omniai.gateway.interceptors

import org.omniai.sdk.core.http.HttpTransportClient
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.MetricsInterceptor
import org.omniai.sdk.interceptors.auth.*

suspend fun defaultGatewayInterceptors(
    configSource: ConfigSource,
    httpClient: HttpTransportClient,
    logger: GatewayLogger = NoOpGatewayLogger
): List<Interceptor> {
    return listOf(
        AuthContextInterceptor(authenticator = loadTokenAuthenticator(configSource, httpClient)),
        RequestLoggingInterceptor(logger),
        MetricsInterceptor()
    )
}
