package org.omniai.sdk.domain.errors

import org.omniai.sdk.domain.common.Provider

sealed interface DomainError {
    val message: String
    val cause: Throwable?
}

data class ParsingError(
    override val message: String,
    override val cause: Throwable? = null,
) : DomainError

data class ApiDownError(
    override val message: String,
    val statusCode: Int? = null,
    override val cause: Throwable? = null,
) : DomainError

data class InvalidRequest(
    override val message: String,
    val statusCode: Int? = null,
    override val cause: Throwable? = null,
) : DomainError

data class ProviderApiError(
    val provider: Provider,
    val statusCode: Int,
    override val message: String,
    override val cause: Throwable? = null,
) : DomainError

data class UnknownDomainError(
    override val message: String,
    override val cause: Throwable? = null,
) : DomainError

