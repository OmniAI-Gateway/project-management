package org.omniai.sdk.interceptors.routing

import org.omniai.sdk.application.pipeline.GatewayContext
import org.omniai.sdk.application.pipeline.Interceptor
import org.omniai.sdk.application.pipeline.InterceptorChain
import org.omniai.sdk.application.pipeline.PipelineResult
import org.omniai.sdk.domain.errors.UnknownDomainError
import org.omniai.sdk.ports.outbound.OutboundPort

class RoutingInterceptor(
    private val outbounds: List<OutboundPort>,
) : Interceptor {
    override suspend fun handle(
        context: GatewayContext,
        chain: InterceptorChain,
    ): PipelineResult {
        if (outbounds.isEmpty()) {
            return PipelineResult.Error(UnknownDomainError("No available outbounds for routing"))
        }

        val selectedOutbound = outbounds.random()

        val newRequest =
            context.request.copy(
                provider = selectedOutbound.provider,
                model = selectedOutbound.model.model,
            )

        val newContext =
            GatewayContext(
                request = newRequest,
                attributes = context.attributes,
                mode = context.mode,
                res = context.res,
            )

        return chain.proceed(newContext)
    }
}
