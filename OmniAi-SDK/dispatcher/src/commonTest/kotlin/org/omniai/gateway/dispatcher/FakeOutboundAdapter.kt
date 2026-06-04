package org.omniai.gateway.dispatcher

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.common.Either
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent
import org.omniai.sdk.ports.outbound.OutboundPort

class FakeOutboundAdapter(
    override val provider: Provider,
    override val model: Model,
    override val key: String = "fake-api-key"
) : OutboundPort {

    // Properties to inspect what was received during the test
    var receivedRequest: CommonRequest? = null

    // Stubbed responses
    var stubbedUnaryResponse: Either<DomainError, CommonResponse>? = null
    var stubbedStreamResponse: Either<DomainError, Flow<CommonResponseEvent>>? = null

    override suspend fun generate(request: CommonRequest): Either<DomainError, CommonResponse> {
        receivedRequest = request
        return stubbedUnaryResponse ?: throw IllegalStateException("Stub response not configured")
    }

    override suspend fun generateStream(request: CommonRequest): Either<DomainError, Flow<CommonResponseEvent>> {
        receivedRequest = request
        return stubbedStreamResponse ?: throw IllegalStateException("Stub response not configured")
    }
}