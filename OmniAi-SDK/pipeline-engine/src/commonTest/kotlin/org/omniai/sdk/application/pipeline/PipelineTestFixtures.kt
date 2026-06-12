package org.omniai.sdk.application.pipeline

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.omniai.sdk.common.Either
import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.common.success
import org.omniai.sdk.common.failure
import org.omniai.sdk.domain.common.CommonRole
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.errors.UnknownDomainError
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.requests.CommonRequestMessage
import org.omniai.sdk.domain.responses.CommonChoice
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent
import org.omniai.sdk.domain.responses.FinishReason
import org.omniai.sdk.domain.common.content.TextPart
import org.omniai.sdk.ports.inbound.DispatcherPort

// ─── Fake request ────────────────────────────────────────────────────────────

fun fakeRequest(model: String = "gpt-4o", provider: Provider = Provider.OPENAI): CommonRequest =
    CommonRequest(
        provider = provider,
        model = model,
        messages = listOf(
            CommonRequestMessage(
                role = CommonRole.USER,
                content = listOf(TextPart("Hello"))
            )
        )
    )

// ─── Fake response ───────────────────────────────────────────────────────────

fun fakeResponse(id: String = "resp-1", model: String = "gpt-4o"): CommonResponse =
    CommonResponse(
        provider = Provider.OPENAI,
        id = id,
        model = model,
        choices = listOf(
            CommonChoice(
                index = 0,
                message = org.omniai.sdk.domain.responses.CommonResponseMessage(
                    role = CommonRole.ASSISTANT,
                    content = listOf(TextPart("Hi there!"))
                ),
                finishReason = FinishReason.STOP
            )
        )
    )

// ─── Fake error ──────────────────────────────────────────────────────────────

fun fakeError(msg: String = "Something went wrong"): DomainError = UnknownDomainError(msg)

// ─── Fake dispatcher implementations ─────────────────────────────────────────

/**
 * Always returns a successful unary response.
 */
class FakeSuccessDispatcher(
    private val response: CommonResponse = fakeResponse()
) : DispatcherPort {
    var generateCalled = false
    var generateStreamCalled = false

    override suspend fun generate(request: CommonRequest, attributes: TypedMap): Either<DomainError, CommonResponse> {
        generateCalled = true
        return success(response)
    }

    override suspend fun generateStream(
        request: CommonRequest,
        attributes: TypedMap
    ): Either<DomainError, Flow<CommonResponseEvent>> {
        generateStreamCalled = true
        return success(emptyFlow())
    }
}

/**
 * Always returns a failure.
 */
class FakeFailureDispatcher(
    private val error: DomainError = fakeError()
) : DispatcherPort {
    override suspend fun generate(request: CommonRequest, attributes: TypedMap): Either<DomainError, CommonResponse> =
        failure(error)

    override suspend fun generateStream(
        request: CommonRequest,
        attributes: TypedMap
    ): Either<DomainError, Flow<CommonResponseEvent>> =
        failure(error)
}

// ─── Helper builder ───────────────────────────────────────────────────────────

fun buildPipeline(
    dispatcher: DispatcherPort = FakeSuccessDispatcher(),
    configure: GatewayPipelineBuilder.() -> Unit = {}
): GatewayPipeline = GatewayPipelineBuilder().apply {
    configure()
    installDispatcher(dispatcher)
}.build()

fun fakeContext(
    mode: RequestMode = RequestMode.UNARY,
    res: PipelineResult = PipelineResult.NoResult
): GatewayContext = GatewayContext(
    request = fakeRequest(),
    mode = mode,
    res = res
)
