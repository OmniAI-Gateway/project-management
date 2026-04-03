package org.omniai.sdk.core.pipeline


import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.domain.requests.CommonRequest

data class GatewayContext(
    val request: CommonRequest,
    val res: PipelineResult? = null,
    val attributes: TypedMap = TypedMap()
)