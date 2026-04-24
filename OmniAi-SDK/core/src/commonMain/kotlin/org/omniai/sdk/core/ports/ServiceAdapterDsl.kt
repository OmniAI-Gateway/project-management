package org.omniai.sdk.core.ports

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.core.commom.Either
import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

/**
 * Lightweight service contract built via DSL so SDK consumers avoid boilerplate classes.
 */
interface ServiceAdapter : InferenceServicePort

class ServiceAdapterBuilder {
    private var unaryHandler: (suspend (CommonRequest, TypedMap) -> Either<DomainError, CommonResponse>)? = null
    private var streamHandler: (suspend (CommonRequest, TypedMap) -> Either<DomainError, Flow<CommonResponseEvent>>)? = null

    fun unary(handler: suspend (CommonRequest) -> Either<DomainError, CommonResponse>) {
        unaryHandler = { request, _ -> handler(request) }
    }

    fun unary(handler: suspend (CommonRequest, TypedMap) -> Either<DomainError, CommonResponse>) {
        unaryHandler = handler
    }

    fun stream(handler: suspend (CommonRequest) -> Either<DomainError, Flow<CommonResponseEvent>>) {
        streamHandler = { request, _ -> handler(request) }
    }

    fun stream(handler: suspend (CommonRequest, TypedMap) -> Either<DomainError, Flow<CommonResponseEvent>>) {
        streamHandler = handler
    }

    fun build(): ServiceAdapter {
        val unary = requireNotNull(unaryHandler) {
            "Missing unary handler. Configure it with unary { request -> ... }."
        }
        val stream = requireNotNull(streamHandler) {
            "Missing stream handler. Configure it with stream { request -> ... }."
        }

        return object : ServiceAdapter {
            override suspend fun generate(request: CommonRequest, attributes: TypedMap): Either<DomainError, CommonResponse> =
                unary(request, attributes)

            override suspend fun generateStream(request: CommonRequest, attributes: TypedMap): Either<DomainError, Flow<CommonResponseEvent>> =
                stream(request, attributes)
        }
    }
}

fun serviceAdapter(block: ServiceAdapterBuilder.() -> Unit): ServiceAdapter =
    ServiceAdapterBuilder().apply(block).build()
