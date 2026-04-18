package org.omniai.sdk.core.http

import org.omniai.sdk.core.commom.TypedMap

sealed class HttpCallResult<out T> {
    abstract val metadata: TypedMap

    data class Success<out T>(
        val data: T,
        override val metadata: TypedMap = TypedMap()
    ) : HttpCallResult<T>()

    data class NetworkError(
        val exception: Throwable,
        override val metadata: TypedMap = TypedMap()
    ) : HttpCallResult<Nothing>()

    data class ApiError(
        val code: Int,
        val message: String? = null,
        override val metadata: TypedMap = TypedMap()
    ) : HttpCallResult<Nothing>()

    data class SerializationError(
        val exception: Throwable,
        override val metadata: TypedMap = TypedMap()
    ) : HttpCallResult<Nothing>()

    data class UnknownError(
        val exception: Throwable,
        override val metadata: TypedMap = TypedMap()
    ) : HttpCallResult<Nothing>()
}
