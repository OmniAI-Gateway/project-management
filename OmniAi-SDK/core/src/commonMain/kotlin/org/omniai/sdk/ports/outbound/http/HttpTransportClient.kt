package org.omniai.sdk.ports.outbound.http

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer
import org.omniai.sdk.binders.IncomingContext
import org.omniai.sdk.common.TypedMap

interface HttpTransportClient {
    fun bindResponseMetadata(
        context: IncomingContext,
        headerNames: Set<String>,
    ): TypedMap

    suspend fun <T, V> execute(
        config: RequestConfig<V>,
        responseSerializer: KSerializer<T>,
    ): HttpCallResult<T>

    fun <T, V> listen(
        config: RequestConfig<V>,
        eventName: String?,
        responseSerializer: KSerializer<T>,
    ): Flow<HttpCallResult<T>>

    fun <E : Any, V> listenMany(
        config: RequestConfig<V>,
        serializersByEvent: Map<String, KSerializer<out E>>,
    ): Flow<HttpCallResult<E>>
}
