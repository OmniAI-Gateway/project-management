package org.omniai.sdk.interceptors.ratelimiting.policy

import org.omniai.sdk.application.pipeline.GatewayContext
import org.omniai.sdk.interceptors.ratelimiting.domain.RateLimitTarget

/**
 * Defines a generic policy for extracting context data and resolving rate limits.
 *
 * The client is responsible for implementing this interface, providing a strongly
 * typed extraction of data of type [T] from the [GatewayContext], and mapping
 * that data into a list of [RateLimitTarget] to be evaluated by the interceptor.
 *
 * @param T The domain-specific type representing the extracted context data.
 */
interface RateLimitPolicy<T : Any> {
    /**
     * Extracts relevant data from the incoming GatewayContext.
     *
     * @param context The current request context traversing the pipeline.
     * @return An instance of [T] if the data is present and applicable, or null if this policy should be skipped.
     */
    suspend fun extract(context: GatewayContext): T?

    /**
     * Evaluates the extracted data and returns a list of rate limit targets.
     *
     * @param data The data previously extracted by [extract].
     * @return A list of [RateLimitTarget] defining the bounds and keys for this request.
     */
    suspend fun evaluate(data: T): List<RateLimitTarget>
}
