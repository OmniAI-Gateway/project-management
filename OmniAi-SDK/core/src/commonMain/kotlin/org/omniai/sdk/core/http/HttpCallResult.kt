package org.omniai.sdk.core.http

sealed class HttpCallResult<out T> {
    data class Success<out T>(val data: T) : HttpCallResult<T>()
    data class NetworkError(val exception: Throwable) : HttpCallResult<Nothing>()
    data class ApiError(val code: Int, val message: String? = null) : HttpCallResult<Nothing>()
    data class SerializationError(val exception: Throwable) : HttpCallResult<Nothing>()
    data class UnknownError(val exception: Throwable) : HttpCallResult<Nothing>()
}
