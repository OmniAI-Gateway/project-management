package org.omniai.sdk.application.pipeline

import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.domain.requests.CommonRequest

enum class RequestMode {
    UNARY,
    STREAM
}

data class GatewayContext(
    val request: CommonRequest,
    val mode: RequestMode = RequestMode.UNARY,
    val res: PipelineResult = PipelineResult.NoResult,
    val attributes: TypedMap = TypedMap()
)