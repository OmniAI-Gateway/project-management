package org.omniai.sdk.interceptors.helper

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.omniai.sdk.application.pipeline.GatewayContext
import org.omniai.sdk.application.pipeline.InterceptorChain
import org.omniai.sdk.application.pipeline.PipelineResult
import org.omniai.sdk.application.pipeline.RequestMode
import org.omniai.sdk.common.Either
import org.omniai.sdk.common.success
import org.omniai.sdk.domain.common.CommonRole
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.errors.UnknownDomainError
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.requests.CommonRequestMessage
import org.omniai.sdk.domain.responses.CommonChoice
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent
import org.omniai.sdk.domain.responses.CommonResponseMessage
import org.omniai.sdk.domain.responses.FinishReason
import org.omniai.sdk.ports.outbound.OutboundPort

// ─── Context helpers ──────────────────────────────────────────────────────────

fun fakeRequest(
    provider: Provider = Provider.OPENAI,
    model: String = "gpt-4o",
): CommonRequest =
    CommonRequest(
        provider = provider,
        model = model,
        messages = listOf(CommonRequestMessage(role = CommonRole.USER, content = emptyList())),
    )

fun fakeContext(
    provider: Provider = Provider.OPENAI,
    model: String = "gpt-4o",
    mode: RequestMode = RequestMode.UNARY,
): GatewayContext =
    GatewayContext(
        request = fakeRequest(provider, model),
        mode = mode,
    )

// ─── Response helpers ─────────────────────────────────────────────────────────

fun fakeResponse(
    id: String = "resp-1",
    provider: Provider = Provider.OPENAI,
    model: String = "gpt-4o",
): CommonResponse =
    CommonResponse(
        provider = provider,
        id = id,
        model = model,
        choices =
            listOf(
                CommonChoice(
                    index = 0,
                    message = CommonResponseMessage(role = CommonRole.ASSISTANT, content = emptyList()),
                    finishReason = FinishReason.STOP,
                ),
            ),
    )

fun fakeError(msg: String = "upstream error"): DomainError = UnknownDomainError(msg)

// ─── Chain helpers ────────────────────────────────────────────────────────────

/** Chain that always returns the given result, ignoring context. */
class StaticChain(
    private val result: PipelineResult,
) : InterceptorChain {
    var callCount = 0

    override suspend fun proceed(context: GatewayContext): PipelineResult {
        callCount++
        return result
    }
}

/** Chain that captures the context passed to it, then returns the given result. */
class CapturingChain(
    private val result: PipelineResult = PipelineResult.Unary(fakeResponse()),
) : InterceptorChain {
    var lastContext: GatewayContext? = null

    override suspend fun proceed(context: GatewayContext): PipelineResult {
        lastContext = context
        return result
    }
}

// ─── Fake OutboundPort ────────────────────────────────────────────────────────

class FakeOutbound(
    override val provider: Provider,
    modelName: String,
    override val key: String = "${provider.value}:$modelName",
    private val unaryResult: Either<DomainError, CommonResponse> =
        success(
            fakeResponse(
                provider = provider,
                model = modelName,
            ),
        ),
    private val streamResult: Either<DomainError, Flow<CommonResponseEvent>> = success(emptyFlow()),
) : OutboundPort {
    override val model: Model = Model(modelName)

    override suspend fun generate(request: CommonRequest): Either<DomainError, CommonResponse> = unaryResult

    override suspend fun generateStream(request: CommonRequest): Either<DomainError, Flow<CommonResponseEvent>> = streamResult
}
