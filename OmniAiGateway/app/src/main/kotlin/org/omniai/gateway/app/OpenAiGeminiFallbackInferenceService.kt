package org.omniai.gateway.app

import kotlinx.coroutines.flow.Flow
import org.omniai.sdk.adapters.gemini.GeminiOutboundAdapter
import org.omniai.sdk.adapters.openai.OpenAiOutboundAdapter
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

/**
 * Uses OpenAI as primary provider and falls back to Gemini when OpenAI fails.
 */
class OpenAiGeminiFallbackInferenceService(
    private val openAiOutboundAdapter: OpenAiOutboundAdapter,
    private val geminiOutboundAdapter: GeminiOutboundAdapter,
    private val geminiFallbackModel: String
) : InferenceServicePort {

    override suspend fun generate(request: CommonRequest): CommonResponse {
//        return try {
//            openAiOutboundAdapter.generate(request)
//        } catch (openAiError: Exception) {
//            println("OpenAI request failed, falling back to Gemini: ${openAiError.message}")
//            geminiOutboundAdapter.generate(request.toGeminiFallbackRequest(geminiFallbackModel))
//        }

       return try {
           geminiOutboundAdapter.generate(request.toGeminiFallbackRequest(geminiFallbackModel))
       }catch (e: Exception){
           println(e.message + e.cause)
           throw e
       }
    }

    override fun generateStream(request: CommonRequest): Flow<CommonResponseEvent> {
        return try {
            openAiOutboundAdapter.generateStream(request)
        } catch (openAiError: Exception) {
            println("OpenAI stream setup failed, falling back to Gemini: ${openAiError.message}")
            geminiOutboundAdapter.generateStream(request.toGeminiFallbackRequest(geminiFallbackModel))
        }
    }
}

private fun CommonRequest.toGeminiFallbackRequest(geminiModel: String): CommonRequest =
    copy(
        provider = Provider.GEMINI,
        model = geminiModel
    )

