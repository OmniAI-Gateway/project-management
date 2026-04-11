package org.omniai.gateway.services

import org.omniai.gateway.interceptors.defaultGatewayInterceptors
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.gatewayPipeline
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.core.ports.OutboundPort

fun gatewayServiceAssembler(
    outbounds: List<OutboundPort>,
    interceptors: List<Interceptor> = defaultGatewayInterceptors(),
): InferenceServicePort {
    val terminal = routingInferenceServiceFactory(outbounds)
    val pipeline = gatewayPipeline {
        interceptors.forEach(::install)
        installService(terminal)
    }
    return PipelineBackedInferenceService(
        pipeline = pipeline
    )
}

