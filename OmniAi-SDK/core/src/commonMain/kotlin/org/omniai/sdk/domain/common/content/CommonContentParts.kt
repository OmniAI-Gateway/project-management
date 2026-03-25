package org.omniai.sdk.domain.common.content

import org.omniai.sdk.domain.common.json.JsonObjectMap
import org.omniai.sdk.domain.common.json.JsonValue

sealed interface RequestContentPart

sealed interface ResponseContentPart

/**
 * Shared content can flow in both directions (request history and model responses).
 */
sealed interface SharedContentPart : RequestContentPart, ResponseContentPart

data class TextPart(
    val text: String
) : SharedContentPart

data class JsonPart(
    val json: JsonValue
) : SharedContentPart

data class ToolCallPart(
    val toolCallId: String,
    val functionName: String,
    val argumentsJson: JsonObjectMap
) : SharedContentPart

data class ToolResultPart(
    val toolCallId: String,
    val content: List<JsonValue>
) : RequestContentPart

data class RefusalPart(
    val reason: String
) : ResponseContentPart

