package org.omniaigateway.domain.responses

import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.content.ResponseContentPart

enum class FinishReason {
    STOP,
    LENGTH,
    TOOL_CALL,
    CONTENT_FILTER,
    OTHER
}

data class CommonUsage(
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val totalTokens: Int? = null
)

data class CommonResponseMessage(
    val role: CommonRole,
    val content: List<ResponseContentPart>
)

data class CommonChoice(
    val index: Int,
    val message: CommonResponseMessage,
    val finishReason: FinishReason? = null
)

data class CommonResponse(
    val provider: Provider,
    val id: String? = null,
    val model: String,
    val choices: List<CommonChoice>,
    val usage: CommonUsage? = null,
    val providerOptions: Map<String, Any?> = emptyMap()
)

