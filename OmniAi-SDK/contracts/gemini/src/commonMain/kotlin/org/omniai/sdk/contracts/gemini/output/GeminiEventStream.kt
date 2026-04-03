package org.omniai.sdk.contracts.gemini.output


/**
 * Wrapper object only, not to be used do serialize or deserialize
 * DOES not Follow the gemini protocol
 */
sealed interface GeminiEventStream {

    data class Chunk(val data: GeminiGenerateContentResponse) : GeminiEventStream

    data object Done : GeminiEventStream

    data class Error(val error: GeminiErrorResponse) : GeminiEventStream
}