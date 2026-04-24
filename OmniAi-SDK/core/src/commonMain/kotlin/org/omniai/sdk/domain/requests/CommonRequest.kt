package org.omniai.sdk.domain.requests

import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.domain.common.CommonGenerationConfig
import org.omniai.sdk.domain.common.CommonRole
import org.omniai.sdk.domain.common.CommonTool
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.common.SystemPrompt
import org.omniai.sdk.domain.common.ToolChoice
import org.omniai.sdk.domain.common.content.RequestContentPart

data class CommonRequestMessage(
    val role: CommonRole,
    val content: List<RequestContentPart>
)

data class CommonRequest(
    val provider: Provider,
    val model: String,
    val messages: List<CommonRequestMessage>,
    val systemPrompt: SystemPrompt? = null,
    val config: CommonGenerationConfig? = null,
    val tools: List<CommonTool> = emptyList(),
    val toolChoice: ToolChoice? = null,
    val jsonResponse: Boolean = false,
    val providerOptions: TypedMap = TypedMap()
)
