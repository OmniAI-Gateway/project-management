package org.omniai.sdk.core.pipeline

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

sealed interface PipelineResult {
    data class Unary(val response: CommonResponse) : PipelineResult
    data class Stream(val eventFlow: Flow<CommonResponseEvent>) : PipelineResult
    data object NoResult: PipelineResult
}

inline fun <T> PipelineResult.fold(
    onUnary: (CommonResponse) -> T,
    onStream: (Flow<CommonResponseEvent>) -> T,
    onNothing: () -> T
): T = when (this) {
    is PipelineResult.Unary -> onUnary(response)
    is PipelineResult.Stream -> onStream(eventFlow)
    is PipelineResult.NoResult -> onNothing()
}

/**
 * Extension to safely extract a Unary response.
 * Throws IllegalStateException if the result is a Stream.
 */
@Throws(IllegalStateException::class)
fun PipelineResult.requireUnaryResponse(): CommonResponse = when (this) {
    is PipelineResult.Unary -> response
    else -> error("Contract violation: Expected a Unary response, but received a Stream.")
}

/**
 * Extension to safely extract a Stream flow.
 * Throws IllegalStateException if the result is Unary.
 */
@Throws(IllegalStateException::class)
fun PipelineResult.requireStreamEvents(): Flow<CommonResponseEvent> = when (this) {
    is PipelineResult.Stream -> eventFlow
    else -> error("Contract violation: Expected a Stream response, but received a Unary result.")
}
