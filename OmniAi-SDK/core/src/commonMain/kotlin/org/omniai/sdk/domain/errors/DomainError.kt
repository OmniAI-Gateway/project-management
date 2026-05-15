package org.omniai.sdk.domain.errors

import org.omniai.sdk.domain.common.Provider

sealed interface DomainError {
    val message: String
    val cause: Throwable?
    val code: ErrorCode
}

data class ParsingError(
    override val message: String,
    override val cause: Throwable? = null,
    override val code: ErrorCode = ErrorCode.INTERNAL_SERVER_ERROR
) : DomainError

data class ApiDownError(
    override val message: String,
    val statusCode: Int? = null,
    override val cause: Throwable? = null,
    override val code: ErrorCode = ErrorCode.SERVICE_UNAVAILABLE
) : DomainError

data class InvalidRequest(
    override val message: String,
    val statusCode: Int? = null,
    override val cause: Throwable? = null,
    override val code: ErrorCode = ErrorCode.BAD_REQUEST
) : DomainError

data class ProviderApiError(
    val provider: Provider,
    val statusCode: Int,
    override val message: String,
    override val cause: Throwable? = null,
    override val code: ErrorCode = ErrorCode.PROVIDER_ERROR
) : DomainError

data class UnknownDomainError(
    override val message: String,
    override val cause: Throwable? = null,
    override val code: ErrorCode = ErrorCode.UNKNOWN_ERROR
) : DomainError

data class UnauthorizedError(
    override val message: String,
    override val cause: Throwable? = null,
    override val code: ErrorCode = ErrorCode.UNAUTHORIZED
) : DomainError

data class ForbiddenError(
    override val message: String,
    override val cause: Throwable? = null,
    override val code: ErrorCode = ErrorCode.FORBIDDEN
) : DomainError

data class NotFoundError(
    override val message: String,
    override val cause: Throwable? = null,
    override val code: ErrorCode = ErrorCode.NOT_FOUND
) : DomainError

data class TimeoutError(
    override val message: String,
    override val cause: Throwable? = null,
    override val code: ErrorCode = ErrorCode.TIMEOUT
) : DomainError

data class ConflictError(
    override val message: String,
    override val cause: Throwable? = null,
    override val code: ErrorCode = ErrorCode.CONFLICT
) : DomainError
