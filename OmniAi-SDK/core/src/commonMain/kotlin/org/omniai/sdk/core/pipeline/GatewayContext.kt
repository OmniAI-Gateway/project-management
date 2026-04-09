package org.omniai.sdk.core.pipeline

import org.omniai.sdk.core.commom.TypedMap
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