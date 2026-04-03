package org.omniai.gateway.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.omniai.sdk.core.commom.AttributeKey
import org.omniai.sdk.core.commom.key
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.core.ports.OutboundPort
import org.omniai.sdk.core.ports.serviceAdapter
import org.omniai.sdk.domain.requests.CommonRequest

object Keys {
    val ROUTING_OUTBOUND_INDEX: AttributeKey<Int> = key("routing.outbound.index")
    val ROUTING_ATTEMPTED_OUTBOUNDS: AttributeKey<MutableList<String>> = key("routing.outbound.attempted")
}

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
    val startIndex = request.providerOptions.getOrPut(Keys.ROUTING_OUTBOUND_INDEX) { 0 }
    val attempted = request.providerOptions.getOrPut(Keys.ROUTING_ATTEMPTED_OUTBOUNDS) { mutableListOf() }

    var lastError: Throwable? = null
    var index = startIndex

    while (index < outbounds.size) {
        val outbound = outbounds[index]
        request.providerOptions[Keys.ROUTING_OUTBOUND_INDEX] = index
        attempted += "${outbound.provider.value}:${outbound.model.model}"

        try {
            return action(outbound, request)
        } catch (ex: Throwable) {
            lastError = ex
            index += 1
            request.providerOptions[Keys.ROUTING_OUTBOUND_INDEX] = index
        }
    }
    throw IllegalStateException(
        "No outbound succeeded for provider=${request.provider.value} model=${request.model}",
        lastError
    )
}
