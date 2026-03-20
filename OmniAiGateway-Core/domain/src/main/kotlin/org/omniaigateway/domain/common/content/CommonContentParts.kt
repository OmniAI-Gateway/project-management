package org.omniaigateway.domain.common.content

sealed interface RequestContentPart

sealed interface ResponseContentPart

/**
 * Shared content can flow in both directions (request history and model responses).
 */
sealed interface SharedContentPart : RequestContentPart, ResponseContentPart

data class TextPart(
    val text: String
) : SharedContentPart

data class ToolCallPart(
    val toolCallId: String,
    val functionName: String,
    val argumentsJson: Map<String, Any?>
) : SharedContentPart

data class ToolResultPart(
    val toolCallId: String,
    val content: List<Any?>
) : RequestContentPart

data class RefusalPart(
    val reason: String
) : ResponseContentPart

