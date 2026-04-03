package org.omniai.gateway.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.core.ports.OutboundPort
import org.omniai.sdk.core.ports.serviceAdapter
import org.omniai.sdk.domain.requests.CommonRequest


fun routingInferenceServiceFactory(outbounds: List<OutboundPort>): InferenceServicePort {
    return serviceAdapter {
        unary { request ->
            generateWithFallback(request, outbounds)
        }
        stream { request ->
            generateStreamWithFallback(request, outbounds)
        }
    }
}

private suspend fun generateWithFallback(request: CommonRequest, outbounds: List<OutboundPort>) =
    tryOutbounds(request, outbounds) { outbound, req ->
        outbound.generate(req)
    }

private fun generateStreamWithFallback(request: CommonRequest, outbounds: List<OutboundPort>): Flow<org.omniai.sdk.domain.responses.CommonResponseEvent> =
    flow {
        emitAll(tryOutbounds(request, outbounds) { outbound, req -> outbound.generateStream(req) })
    }

private suspend fun <T> tryOutbounds(
    request: CommonRequest,
    outbounds: List<OutboundPort>,
    action: suspend (OutboundPort, CommonRequest) -> T
): T {
    var lastError: Throwable? = null

    for (outbound in outbounds) {
        try {
            return action(outbound, request)
        } catch (ex: Throwable) {
            lastError = ex
        }
    }

    throw IllegalStateException(
        "No outbound succeeded for provider=${request.provider.value} model=${request.model}",
        lastError
    )
}