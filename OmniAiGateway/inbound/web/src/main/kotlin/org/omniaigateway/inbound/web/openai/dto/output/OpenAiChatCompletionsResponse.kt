package org.omniaigateway.inbound.web.openai.dto.output

import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.inbound.web.DomainMappableOut
import org.omniaigateway.inbound.web.openai.mapper.toOpenAiChatCompletionsResponse

data class OpenAiChatCompletionsResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val systemFingerprint: String? = null,
    val choices: List<OpenAiChoice>,
    val usage: OpenAiUsage? = null,
) {
    companion object : DomainMappableOut<CommonResponse, OpenAiChatCompletionsResponse> {
        override fun fromDomain(domain: CommonResponse): OpenAiChatCompletionsResponse =
            domain.toOpenAiChatCompletionsResponse()
    }
}
