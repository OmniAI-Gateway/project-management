package org.omniai.sdk.gateway.client.dsl.interceptors

import org.omniai.sdk.interceptors.ratelimiting.RateLimitInterceptor
import org.omniai.sdk.interceptors.ratelimiting.policy.RateLimitPolicy
import org.omniai.sdk.interceptors.ratelimiting.store.InMemoryRateLimitStore
import org.omniai.sdk.interceptors.ratelimiting.store.RateLimitStore
import org.omniai.sdk.gateway.client.dsl.GatewayDsl

@GatewayDsl
class RateLimitingInterceptorBuilder {

    var store: RateLimitStore = InMemoryRateLimitStore()
    
    private val policies = mutableListOf<RateLimitPolicy<out Any>>()

    fun addPolicy(policy: RateLimitPolicy<out Any>) {
        policies.add(policy)
    }

    internal fun build(): RateLimitInterceptor {
        return RateLimitInterceptor(store = store, policies = policies.toList())
    }
}
