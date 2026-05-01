package org.omniai.sdk.gateway.client.dsl.interceptors

import org.omniai.sdk.interceptors.ratelimiting.RateLimitInterceptor
import org.omniai.sdk.interceptors.ratelimiting.policy.RateLimitPolicy
import org.omniai.sdk.interceptors.ratelimiting.store.InMemoryRateLimitStore
import org.omniai.sdk.interceptors.ratelimiting.store.RateLimitStore

/**
 * DSL Builder for configuring a [RateLimitInterceptor].
 */
class RateLimitingInterceptorBuilder {
    /**
     * The storage backend to use for rate limiting.
     * Defaults to [InMemoryRateLimitStore].
     */
    var store: RateLimitStore = InMemoryRateLimitStore()
    
    private val policies = mutableListOf<RateLimitPolicy<out Any>>()

    /**
     * Registers a new [RateLimitPolicy] to be evaluated during interceptor execution.
     */
    fun addPolicy(policy: RateLimitPolicy<out Any>) {
        policies.add(policy)
    }

    /**
     * Builds the [RateLimitInterceptor] with the configured store and policies.
     */
    internal fun build(): RateLimitInterceptor {
        return RateLimitInterceptor(store = store, policies = policies.toList())
    }
}
