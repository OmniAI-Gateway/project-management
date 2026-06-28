package org.omniai.sdk.contracts.gemini.output

sealed interface GeminiEventStream {
    data class Chunk(
        val data: GeminiGenerateContentResponse,
    ) : GeminiEventStream

    data object Done : GeminiEventStream

    data class Error(
        val error: GeminiErrorResponse,
    ) : GeminiEventStream
}
