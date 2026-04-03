package org.omniai.sdk.core.pipeline

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.core.commom.Either
import org.omniai.sdk.core.commom.failure
import org.omniai.sdk.core.commom.success
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.errors.UnknownDomainError
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

sealed interface PipelineResult {
    data class Unary(val response: CommonResponse) : PipelineResult
    data class Stream(val eventFlow: Flow<CommonResponseEvent>) : PipelineResult
    data class Error(val error: DomainError) : PipelineResult
    data object NoResult : PipelineResult
}

inline fun <T> PipelineResult.fold(
    onUnary: (CommonResponse) -> T,
    onStream: (Flow<CommonResponseEvent>) -> T,
    onError: (DomainError) -> T,
    onNothing: () -> T,
): T = when (this) {
    is PipelineResult.Unary -> onUnary(response)
    is PipelineResult.Stream -> onStream(eventFlow)
    is PipelineResult.Error -> onError(error)
    is PipelineResult.NoResult -> onNothing()
}

fun PipelineResult.requireUnaryResponse(): Either<DomainError, CommonResponse> = when (this) {
    is PipelineResult.Unary -> success(response)
    is PipelineResult.Error -> failure(error)
    else -> failure(UnknownDomainError("Contract violation: Expected unary response but got stream/no-result"))
}

fun PipelineResult.requireStreamEvents(): Either<DomainError, Flow<CommonResponseEvent>> = when (this) {
    is PipelineResult.Stream -> success(eventFlow)
    is PipelineResult.Error -> failure(error)
    else -> failure(UnknownDomainError("Contract violation: Expected stream response but got unary/no-result"))
}
