package org.omniai.gateway.services

import org.omniai.sdk.interceptors.defaultGatewayInterceptors
import org.omniai.sdk.core.http.HttpTransportClient
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.gatewayPipeline
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.core.ports.OutboundPort

suspend fun gatewayServiceAssembler(
    outbounds: List<OutboundPort>,
    configSource: ConfigSource? = null,
    httpClient: HttpTransportClient? = null,
    interceptors: List<Interceptor>?,
): InferenceServicePort {
    val actualInterceptors = interceptors ?: defaultGatewayInterceptors(configSource!!, httpClient!!) //mudar depois
    val terminal = routingInferenceServiceFactory(outbounds)
    val pipeline = gatewayPipeline {
        actualInterceptors.forEach(::install)
        installService(terminal)
    }
    return PipelineBackedInferenceService(
        pipeline = pipeline
    )
}
