package org.omniai.gateway.inbound.web

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.gemini.input.GeminiGenerateContentRequest
import org.omniai.sdk.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniai.sdk.core.commom.TypedMap
import org.omniai.sdk.inbound.anthropic.AnthropicInboundAdapter
import org.omniai.sdk.inbound.gemini.GEMINI_MODEL_KEY
import org.omniai.sdk.inbound.gemini.GeminiInboundAdapter
import org.omniai.sdk.inbound.openai.OpenAiInboundAdapter

data class GatewayInboundAdapters(
	val anthropic: AnthropicInboundAdapter,
	val openAi: OpenAiInboundAdapter,
	val gemini: GeminiInboundAdapter
)

fun Application.installGatewayRoutes(
	json: Json,
	adapters: GatewayInboundAdapters
) {
	routing {
		post("/v1/messages") {
			handleAnthropicMessages(call, json, adapters)
		}

		post("/v1/chat/completions") {
			handleOpenAiCompletions(call, json, adapters)
		}

		post("/v1beta/models/{model}:generateContent") {
			handleGeminiGenerateContent(call, json, adapters)
		}
	}
}

private suspend fun handleAnthropicMessages(
	call: ApplicationCall,
	json: Json,
	adapters: GatewayInboundAdapters
) {
	val request = parseBody<AnthropicMessagesRequest>(call, json) ?: return

	val response = runCatching {
		adapters.anthropic.generate(request, TypedMap())
	}.getOrElse { e ->
		respondUnexpectedError(call, e)
		return
	}

	call.respond(HttpStatusCode.OK, response)
}

private suspend fun handleOpenAiCompletions(
	call: ApplicationCall,
	json: Json,
	adapters: GatewayInboundAdapters
) {
	val request = parseBody<OpenAiChatCompletionsRequest>(call, json) ?: return
	if (request.stream == true) {
		call.respond(
			HttpStatusCode.NotImplemented,
			mapOf("error" to "stream=true is not available yet for /v1/chat/completions")
		)
		return
	}

	runCatching {
		adapters.openAi.generate(request, TypedMap())
	}.onSuccess {
		call.respond(HttpStatusCode.OK, it)
	}.onFailure { e ->
		respondUnexpectedError(call, e)
	}
}

private suspend fun handleGeminiGenerateContent(
	call: ApplicationCall,
	json: Json,
	adapters: GatewayInboundAdapters
) {
	val request = parseBody<GeminiGenerateContentRequest>(call, json) ?: return
	val model = call.parameters["model"]
	val map = TypedMap().also {
		if (!model.isNullOrBlank()) {
			it.put(GEMINI_MODEL_KEY, model)
		}
	}

	runCatching {
		adapters.gemini.generate(request, map)
	}.onSuccess {
		call.respond(HttpStatusCode.OK, it)
	}.onFailure { e ->
		respondUnexpectedError(call, e)
	}
}

private suspend inline fun <reified T> parseBody(
	call: ApplicationCall,
	json: Json
): T? {
	val rawBody = call.receiveText()
	return try {
		json.decodeFromString<T>(rawBody)
	} catch (e: SerializationException) {
		call.respond(
			HttpStatusCode.BadRequest,
			mapOf("error" to "Invalid request body", "details" to (e.message ?: "Serialization error"))
		)
		null
	} catch (e: Exception) {
		call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request body"))
		null
	}
}

private suspend fun respondUnexpectedError(call: ApplicationCall, throwable: Throwable) {
	println("[gateway] request failed: ${throwable.message}")
	call.respond(
		HttpStatusCode.InternalServerError,
		mapOf("error" to "Internal server error")
	)
}

