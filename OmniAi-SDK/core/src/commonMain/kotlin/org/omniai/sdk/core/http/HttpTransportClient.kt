package org.omniai.sdk.core.http

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer

interface HttpTransportClient {

    suspend fun <T, V> execute(
        config: RequestConfig<V>,
        responseSerializer: KSerializer<T>
    ): HttpCallResult<T>

    fun <T> listen(
        config: RequestConfig<Unit>,
        eventName: String?,
        responseSerializer: KSerializer<T>
    ): Flow<HttpCallResult<T>>

    fun <E : Any> listenMany(
        config: RequestConfig<Unit>,
        serializersByEvent: Map<String, KSerializer<out E>>,
    ): Flow<HttpCallResult<E>>
}
