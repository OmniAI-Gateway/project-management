package org.omniai.gateway.services

import org.omniai.gateway.interceptors.defaultGatewayInterceptors
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.ProviderModelMetrics
import org.omniai.sdk.core.pipeline.gatewayPipeline
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.core.ports.OutboundPort
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.requests.CommonRequest


fun gatewayServiceAssembler(
    outbounds: List<OutboundPort>,
    fallbackProvider: Provider? = null,
    interceptors: List<Interceptor> = defaultGatewayInterceptors(),
    onMetricsCaptured: (CommonRequest, ProviderModelMetrics) -> Unit = { _, _ -> }
): InferenceServicePort {
    val terminal = RoutingInferenceServiceFactory.create(
        outbounds = outbounds,
        fallbackProvider = fallbackProvider
    )

    val pipeline = gatewayPipeline {
        interceptors.forEach(::install)
        installService(terminal)
    }

    return PipelineBackedInferenceService(
        pipeline = pipeline,
        onMetricsCaptured = onMetricsCaptured
    )
}


