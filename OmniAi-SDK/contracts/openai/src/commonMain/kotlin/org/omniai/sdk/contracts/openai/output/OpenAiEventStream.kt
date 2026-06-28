package org.omniai.sdk.contracts.openai.output

sealed interface OpenAiEventStream {
    data class Chunk(
        val data: OpenAiChatCompletionsResponse,
    ) : OpenAiEventStream

    data object Done : OpenAiEventStream

    data class Error(
        val error: OpenAiError,
    ) : OpenAiEventStream
}
