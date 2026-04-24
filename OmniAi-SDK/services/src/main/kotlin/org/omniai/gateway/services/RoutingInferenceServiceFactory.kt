package org.omniai.gateway.services

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.core.commom.Either
import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.core.commom.failure
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.core.ports.OutboundPort
import org.omniai.sdk.core.ports.serviceAdapter
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.errors.UnknownDomainError
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

fun routingInferenceServiceFactory(outbounds: List<OutboundPort>): InferenceServicePort {
    val router = LatencyRouter()
    return serviceAdapter {
        unary { request: CommonRequest, attributes: TypedMap ->
            val ordered = router.orderOutbounds(request, outbounds)
            generateWithFallback(request, attributes, ordered)
        }
        stream { request: CommonRequest, attributes: TypedMap ->
            val ordered = router.orderOutbounds(request, outbounds)
            generateStreamWithFallback(request, attributes, ordered)
        }
    }
}

private suspend fun generateWithFallback(
    request: CommonRequest,
    attributes: TypedMap,
    outbounds: List<OutboundPort>,
): Either<DomainError, CommonResponse> = tryOutbounds(request, outbounds) { outbound, req ->
    outbound.generate(req.withAttributes(attributes))
}

private suspend fun generateStreamWithFallback(
    request: CommonRequest,
    attributes: TypedMap,
    outbounds: List<OutboundPort>,
): Either<DomainError, Flow<CommonResponseEvent>> = tryOutbounds(request, outbounds) { outbound, req ->
    outbound.generateStream(req.withAttributes(attributes))
}

private fun CommonRequest.withAttributes(attributes: TypedMap): CommonRequest {
    if (attributes.isEmpty()) return this
    val merged = providerOptions.copy().also { it.putAll(attributes) }
    return copy(providerOptions = merged)
}

private suspend fun <T> tryOutbounds(
    request: CommonRequest,
    outbounds: List<OutboundPort>,
    action: suspend (OutboundPort, CommonRequest) -> Either<DomainError, T>,
): Either<DomainError, T> {
    var lastError: DomainError? = null

    for (outbound in outbounds) {
        val outboundRequest = request.copy(
            provider = outbound.provider,
            model = outbound.model.model
        )
        when (val result = action(outbound, outboundRequest)) {
            is Either.Right -> return result
            is Either.Left -> lastError = result.value
        }
    }

    return failure(
        lastError ?: UnknownDomainError(
            message = "No outbound available for provider=${request.provider.value} model=${request.model}"
        )
    )
}

