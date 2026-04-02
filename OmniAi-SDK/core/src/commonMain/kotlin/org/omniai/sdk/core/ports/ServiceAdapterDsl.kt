package org.omniai.sdk.core.ports

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

/**
 * Lightweight service contract built via DSL so SDK consumers avoid boilerplate classes.
 */
interface ServiceAdapter : InferenceServicePort

class ServiceAdapterBuilder {
    private var unaryHandler: (suspend (CommonRequest) -> CommonResponse)? = null
    private var streamHandler: ((CommonRequest) -> Flow<CommonResponseEvent>)? = null

    fun unary(handler: suspend (CommonRequest) -> CommonResponse) {
        unaryHandler = handler
    }

    fun stream(handler: (CommonRequest) -> Flow<CommonResponseEvent>) {
        streamHandler = handler
    }

    fun build(): ServiceAdapter {
        val unary = requireNotNull(unaryHandler) {
            "Missing unary handler. Configure it with unary { request -> ... }."
        }
        val stream = streamHandler ?: { emptyFlow() }

        return object : ServiceAdapter {
            override suspend fun generate(request: CommonRequest): CommonResponse = unary(request)

            override fun generateStream(request: CommonRequest): Flow<CommonResponseEvent> = stream(request)
        }
    }
}

fun serviceAdapter(block: ServiceAdapterBuilder.() -> Unit): ServiceAdapter =
    ServiceAdapterBuilder().apply(block).build()

