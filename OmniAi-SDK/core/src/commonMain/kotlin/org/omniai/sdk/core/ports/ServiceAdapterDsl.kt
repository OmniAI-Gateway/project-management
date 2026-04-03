package org.omniai.sdk.core.ports

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.core.commom.Either
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

/**
 * Lightweight service contract built via DSL so SDK consumers avoid boilerplate classes.
 */
interface ServiceAdapter : InferenceServicePort

class ServiceAdapterBuilder {
    private var unaryHandler: (suspend (CommonRequest) -> Either<DomainError, CommonResponse>)? = null
    private var streamHandler: (suspend (CommonRequest) -> Either<DomainError, Flow<CommonResponseEvent>>)? = null

    fun unary(handler: suspend (CommonRequest) -> Either<DomainError, CommonResponse>) {
        unaryHandler = handler
    }

    fun stream(handler: suspend (CommonRequest) -> Either<DomainError, Flow<CommonResponseEvent>>) {
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
            override suspend fun generate(request: CommonRequest): Either<DomainError, CommonResponse> = unary(request)

            override suspend fun generateStream(request: CommonRequest): Either<DomainError, Flow<CommonResponseEvent>> = stream(request)
        }
    }
}

fun serviceAdapter(block: ServiceAdapterBuilder.() -> Unit): ServiceAdapter =
    ServiceAdapterBuilder().apply(block).build()
