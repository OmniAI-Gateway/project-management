package org.omniaigateway.inbound.web.gemini.dto.output

import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.inbound.web.DomainMappableOut
import org.omniaigateway.inbound.web.gemini.mapper.toGeminiGenerateContentResponse

data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    val usageMetadata: GeminiUsageMetadata? = null,
    val modelVersion: String? = null,
    val responseId: String? = null,
    val promptFeedback: GeminiPromptFeedback? = null
) {
    companion object : DomainMappableOut<CommonResponse, GeminiGenerateContentResponse> {
        override fun fromDomain(domain: CommonResponse): GeminiGenerateContentResponse =
            domain.toGeminiGenerateContentResponse()
    }
}

