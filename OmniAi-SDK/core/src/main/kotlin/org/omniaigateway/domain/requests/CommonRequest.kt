package org.omniaigateway.domain.requests

import org.omniaigateway.domain.common.CommonGenerationConfig
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.CommonTool
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.SystemPrompt
import org.omniaigateway.domain.common.ToolChoice
import org.omniaigateway.domain.common.content.RequestContentPart

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
    val providerOptions: Map<String, Any?> = emptyMap()
)