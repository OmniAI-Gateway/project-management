package org.omniaigateway.adapters.openai

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonElement.Companion.serializer
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.omniaigateway.domain.common.CommonGenerationConfig
import org.omniaigateway.domain.common.CommonRole
import org.omniaigateway.domain.common.CommonTool
import org.omniaigateway.domain.common.Model
import org.omniaigateway.domain.common.Provider
import org.omniaigateway.domain.common.ToolChoice
import org.omniaigateway.domain.common.content.JsonPart
import org.omniaigateway.domain.common.content.RefusalPart
import org.omniaigateway.domain.common.content.ResponseContentPart
import org.omniaigateway.domain.common.content.TextPart
import org.omniaigateway.domain.common.content.ToolCallPart
import org.omniaigateway.domain.common.json.JsonValue
import org.omniaigateway.domain.requests.CommonRequest
import org.omniaigateway.domain.requests.CommonRequestMessage
import org.omniaigateway.domain.responses.CommonChoice
import org.omniaigateway.domain.responses.CommonResponse
import org.omniaigateway.domain.responses.CommonResponseMessage
import org.omniaigateway.domain.responses.CommonUsage

private val prettyJson = Json {
    prettyPrint = true
    explicitNulls = false
    encodeDefaults = false
}

fun main() = runBlocking {
    val apiKey = System.getenv("OPENAI_API_KEY")
        ?: error("Missing OPENAI_API_KEY environment variable")

    val modelName = System.getenv("OPENAI_MODEL") ?: "openai/gpt-oss-120b"

    val baseUrl = System.getenv("OPENAI_BASE_URL") ?: "https://api.groq.com/openai/v1"


    val adapter = OpenAiOutboundAdapter(
        model = Model(modelName),
        apiKey = apiKey,
        baseUrl = baseUrl
    )

    val requests = listOf(
        "simple" to simpleTextRequest(modelName),
        "json-response" to jsonResponseRequest(modelName),
        "tools-auto" to toolsRequest(modelName)
    )

    requests.forEachIndexed { index, (label, request) ->
        println("========== REQUEST #${index + 1} ($label) ==========")
        try {
            val response = adapter.generate(request)
            println(prettyJson.encodeToString(serializer(), response.toJsonElement()))
        } catch (ex: Exception) {
            println(
                prettyJson.encodeToString(
                    serializer(),
                    JsonObject(
                        mapOf(
                            "label" to JsonPrimitive(label),
                            "error" to JsonPrimitive(ex.message ?: ex::class.simpleName ?: "unknown")
                        )
                    )
                )
            )
        }
        println()
    }
}

private fun simpleTextRequest(model: String): CommonRequest =
    CommonRequest(
        provider = Provider.OPENAI,
        model = model,
        messages = listOf(
            CommonRequestMessage(
                role = CommonRole.USER,
                content = listOf(TextPart("Diz olá em português e em inglês."))
            )
        ),
        config = CommonGenerationConfig(temperature = 0.3)
    )

private fun jsonResponseRequest(model: String): CommonRequest =
    CommonRequest(
        provider = Provider.OPENAI,
        model = model,
        messages = listOf(
            CommonRequestMessage(
                role = CommonRole.SYSTEM,
                content = listOf(TextPart("És um assistente especializado em análise e resumo de texto. O teu único objetivo é devolver respostas estritamente num formato JSON válido. Não incluas saudações, explicações adicionais ou blocos de formatação markdown (como ```json) na tua resposta final."))
            ),
            CommonRequestMessage(
                role = CommonRole.USER,
                content = listOf(TextPart("Analisa o texto fornecido abaixo e devolve um objeto JSON com exatamente dois campos:\n1. \"summary\": (string) Um resumo conciso e claro do texto.\n2. \"confidence\": (number) Um valor entre 0.0 e 1.0 que indica o teu nível de confiança de que o resumo captou os pontos principais.\n\nTexto para analisar:\nA inteligência artificial está a mudar a forma como trabalhamos. Ferramentas como modelos de linguagem conseguem analisar grandes volumes de texto, resumir informações e até escrever código. No entanto, é importante que os humanos supervisionem estas ferramentas para garantir a precisão e evitar erros. O futuro do trabalho será uma colaboração entre humanos e máquinas."))
            )
        ),
        jsonResponse = true,
        config = CommonGenerationConfig(
            temperature = 0.0,
            maxTokens = 1000,
            stopSequences = listOf("END")
        )
    )

private fun toolsRequest(model: String): CommonRequest {
    val weatherTool = CommonTool(
        name = "get_weather",
        description = "Obtém o estado do tempo para uma cidade.",
        parametersSchema = mapOf(
            "type" to JsonValue.JsonString("object"),
            "properties" to JsonValue.JsonObject(
                mapOf(
                    "city" to JsonValue.JsonObject(
                        mapOf(
                            "type" to JsonValue.JsonString("string"),
                            "description" to JsonValue.JsonString("Nome da cidade")
                        )
                    ),
                    "unit" to JsonValue.JsonObject(
                        mapOf(
                            "type" to JsonValue.JsonString("string"),
                            "enum" to JsonValue.JsonArray(
                                listOf(JsonValue.JsonString("celsius"), JsonValue.JsonString("fahrenheit"))
                            )
                        )
                    )
                )
            ),
            "required" to JsonValue.JsonArray(listOf(JsonValue.JsonString("city")))
        )
    )

    return CommonRequest(
        provider = Provider.OPENAI,
        model = model,
        messages = listOf(
            CommonRequestMessage(
                role = CommonRole.USER,
                content = listOf(TextPart("Qual está o tempo em Lisboa?"))
            )
        ),
        tools = listOf(weatherTool),
        toolChoice = ToolChoice.Auto,
        config = CommonGenerationConfig(temperature = 0.2)
    )
}

private fun CommonResponse.toJsonElement(): JsonElement =
    JsonObject(
        mapOf(
            "provider" to JsonPrimitive(provider.value),
            "id" to (id?.let(::JsonPrimitive) ?: JsonNull),
            "model" to JsonPrimitive(model),
            "choices" to JsonArray(choices.map(CommonChoice::toJsonElement)),
            "usage" to (usage?.toJsonElement() ?: JsonNull),
            "providerOptions" to providerOptions.toJsonObject()
        )
    )

private fun CommonChoice.toJsonElement(): JsonElement =
    JsonObject(
        mapOf(
            "index" to JsonPrimitive(index),
            "finishReason" to (finishReason?.name?.let(::JsonPrimitive) ?: JsonNull),
            "message" to message.toJsonElement()
        )
    )

private fun CommonResponseMessage.toJsonElement(): JsonElement =
    JsonObject(
        mapOf(
            "role" to JsonPrimitive(role.name),
            "content" to JsonArray(content.map(ResponseContentPart::toJsonElement))
        )
    )

private fun ResponseContentPart.toJsonElement(): JsonElement =
    when (this) {
        is TextPart -> JsonObject(
            mapOf(
                "type" to JsonPrimitive("text"),
                "text" to JsonPrimitive(text)
            )
        )
        is ToolCallPart -> JsonObject(
            mapOf(
                "type" to JsonPrimitive("tool_call"),
                "toolCallId" to JsonPrimitive(toolCallId),
                "functionName" to JsonPrimitive(functionName),
                "argumentsJson" to argumentsJson.toJsonObjectMapElement()
            )
        )
        is JsonPart -> JsonObject(
            mapOf(
                "type" to JsonPrimitive("json"),
                "json" to json.toJsonElement()
            )
        )
        is RefusalPart -> JsonObject(
            mapOf(
                "type" to JsonPrimitive("refusal"),
                "reason" to JsonPrimitive(reason)
            )
        )
    }

private fun CommonUsage.toJsonElement(): JsonElement =
    JsonObject(
        mapOf(
            "inputTokens" to (inputTokens?.let(::JsonPrimitive) ?: JsonNull),
            "outputTokens" to (outputTokens?.let(::JsonPrimitive) ?: JsonNull),
            "totalTokens" to (totalTokens?.let(::JsonPrimitive) ?: JsonNull)
        )
    )

private fun Map<String, Any?>.toJsonObject(): JsonElement =
    JsonObject(entries.associate { (key, value) -> key to value.toJsonElement() })

private fun Any?.toJsonElement(): JsonElement =
    when (this) {
        null -> JsonNull
        is JsonElement -> this
        is String -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Map<*, *> -> JsonObject(entries.associate { (k, v) -> k.toString() to v.toJsonElement() })
        is List<*> -> JsonArray(map { it.toJsonElement() })
        else -> JsonPrimitive(toString())
    }

private fun Map<String, JsonValue>.toJsonObjectMapElement(): JsonElement =
    JsonObject(entries.associate { (key, value) -> key to value.toJsonElement() })

private fun JsonValue.toJsonElement(): JsonElement =
    when (this) {
        is JsonValue.JsonObject -> JsonObject(properties.mapValues { (_, value) -> value.toJsonElement() })
        is JsonValue.JsonArray -> JsonArray(items.map(JsonValue::toJsonElement))
        is JsonValue.JsonString -> JsonPrimitive(value)
        is JsonValue.JsonNumber -> JsonPrimitive(value)
        is JsonValue.JsonBoolean -> JsonPrimitive(value)
        JsonValue.JsonNull -> JsonNull
    }
