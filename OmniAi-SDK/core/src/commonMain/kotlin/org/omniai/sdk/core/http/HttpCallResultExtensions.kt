package org.omniai.sdk.core.http

import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.errors.ApiDownError
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.errors.InvalidRequest
import org.omniai.sdk.domain.errors.ParsingError
import org.omniai.sdk.domain.errors.ProviderApiError
import org.omniai.sdk.domain.errors.UnknownDomainError

fun HttpCallResult<*>.toDomainError(provider: Provider): DomainError = when (this) {
    is HttpCallResult.ApiError -> when (code) {
        in 400..499 -> InvalidRequest(
            message = "${provider.value} rejected request with status $code: ${message.orEmpty()}",
            statusCode = code,
        )

        in 500..599 -> ApiDownError(
            message = "${provider.value} API is unavailable (status $code)",
            statusCode = code,
        )

        else -> ProviderApiError(
            provider = provider,
            statusCode = code,
            message = "${provider.value} API failed with status $code: ${message.orEmpty()}"
        )
    }

    is HttpCallResult.NetworkError -> ApiDownError(
        message = "${provider.value} API request failed due to network issues",
        cause = exception,
    )

    is HttpCallResult.SerializationError -> ParsingError(
        message = "Failed to parse ${provider.value} API response",
        cause = exception,
    )

    is HttpCallResult.UnknownError -> UnknownDomainError(
        message = "Unexpected error while calling ${provider.value}",
        cause = exception,
    )

    is HttpCallResult.Success<*> -> UnknownDomainError(
        message = "toDomainError should not be called on successful results"
    )
}

