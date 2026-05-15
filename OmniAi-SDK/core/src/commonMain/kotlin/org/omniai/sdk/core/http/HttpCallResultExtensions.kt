package org.omniai.sdk.core.http

import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.errors.ApiDownError
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.errors.InvalidRequest
import org.omniai.sdk.domain.errors.ParsingError
import org.omniai.sdk.domain.errors.ProviderApiError
import org.omniai.sdk.domain.errors.UnknownDomainError
import org.omniai.sdk.domain.errors.UnauthorizedError
import org.omniai.sdk.domain.errors.ForbiddenError
import org.omniai.sdk.domain.errors.NotFoundError
import org.omniai.sdk.domain.errors.TimeoutError
import org.omniai.sdk.domain.errors.ConflictError
import org.omniai.sdk.domain.errors.TooManyRequestsError

fun HttpCallResult<*>.toDomainError(provider: Provider): DomainError = when (this) {
    is HttpCallResult.ApiError -> when (code) {
        401 -> UnauthorizedError(message = "${provider.value} rejected request with status 401: ${message.orEmpty()}")
        403 -> ForbiddenError(message = "${provider.value} rejected request with status 403: ${message.orEmpty()}")
        404 -> NotFoundError(message = "${provider.value} rejected request with status 404: ${message.orEmpty()}")
        408 -> TimeoutError(message = "${provider.value} request timed out: ${message.orEmpty()}")
        409 -> ConflictError(message = "${provider.value} request conflict: ${message.orEmpty()}")
        429 -> TooManyRequestsError(message = "${provider.value} rate limit exceeded: ${message.orEmpty()}")
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

