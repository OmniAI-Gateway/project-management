package org.omniai.sdk.domain.responses

import org.omniai.sdk.domain.common.CommonRole
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider

sealed interface CommonResponseEvent {
    val provider: Provider
    val id: String
    val model: Model
    val sequence: Long
    val providerEventType: String?
}

data class ResponseStarted(
    override val provider: Provider,
    override val id: String,
    override val model: Model,
    override val sequence: Long,
    override val providerEventType: String? = null,
) : CommonResponseEvent

data class ChoiceStarted(
    override val provider: Provider,
    override val id: String,
    override val model: Model,
    override val sequence: Long,
    val choiceIndex: Int,
    val role: CommonRole? = null,
    override val providerEventType: String? = null,
) : CommonResponseEvent

data class TextDeltaEvent(
    override val provider: Provider,
    override val id: String,
    override val model: Model,
    override val sequence: Long,
    val choiceIndex: Int,
    val text: String,
    override val providerEventType: String? = null,
) : CommonResponseEvent

data class ToolCallStartedEvent(
    override val provider: Provider,
    override val id: String,
    override val model: Model,
    override val sequence: Long,
    val choiceIndex: Int,
    val toolCallIndex: Int,
    val toolCallId: String,
    val functionName: String,
    override val providerEventType: String? = null,
) : CommonResponseEvent

data class ToolCallArgumentsDeltaEvent(
    override val provider: Provider,
    override val id: String,
    override val model: Model,
    override val sequence: Long,
    val choiceIndex: Int,
    val toolCallIndex: Int,
    val argumentsFragment: String,
    override val providerEventType: String? = null,
) : CommonResponseEvent

data class ChoiceFinished(
    override val provider: Provider,
    override val id: String,
    override val model: Model,
    override val sequence: Long,
    val choiceIndex: Int,
    val finishReason: FinishReason? = null,
    override val providerEventType: String? = null,
) : CommonResponseEvent

data class UsageReported(
    override val provider: Provider,
    override val id: String,
    override val model: Model,
    override val sequence: Long,
    val usage: CommonUsage,
    override val providerEventType: String? = null,
) : CommonResponseEvent

data class ResponseCompleted(
    override val provider: Provider,
    override val id: String,
    override val model: Model,
    override val sequence: Long,
    override val providerEventType: String? = null,
) : CommonResponseEvent

data class ResponseErrored(
    override val provider: Provider,
    override val id: String,
    override val model: Model,
    override val sequence: Long,
    val message: String,
    val retryable: Boolean = false,
    override val providerEventType: String? = null,
) : CommonResponseEvent
