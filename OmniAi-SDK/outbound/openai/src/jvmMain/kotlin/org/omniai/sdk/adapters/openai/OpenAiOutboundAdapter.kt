package org.omniai.sdk.adapters.openai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.omniai.sdk.contracts.openai.output.OpenAiChatCompletionsResponse
import org.omniai.sdk.core.ports.OutboundPort
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent

class OpenAiOutboundAdapter(
    override val model: Model,
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1",
    private val client: HttpClient = defaultClient()
) : OutboundPort {

    private val translator = OpenAiOutboundTranslator()

    override val provider: Provider = Provider.OPENAI

    override suspend fun generate(request: CommonRequest): CommonResponse {
        val providerRequest = translator.fromDomain(request)

        println("REQUEST: ${Json.encodeToJsonElement(providerRequest)}")
        val response = client.post("$baseUrl/chat/completions") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            setBody(providerRequest)
        }
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            throw IllegalStateException(
                "OpenAI request failed with status ${response.status}. body=$errorBody"
            )
        }
        val providerResponse: OpenAiChatCompletionsResponse = response.body()
        println("RESPONSE: ${Json.encodeToJsonElement(providerResponse)}")

        return translator.toDomain(providerResponse)
    }

    override fun generateStream(request: CommonRequest): Flow<CommonResponseEvent> {
        TODO("Not yet implemented")
    }

    companion object {
        private fun defaultClient(): HttpClient =
            HttpClient(OkHttp) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            explicitNulls = false
                            encodeDefaults = false
                        }
                    )
                }
            }
    }
}
