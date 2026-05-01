package org.omniai.sdk.interceptors.ratelimiting

import org.omniai.sdk.core.pipeline.GatewayContext
import org.omniai.sdk.core.pipeline.Interceptor
import org.omniai.sdk.core.pipeline.InterceptorChain
import org.omniai.sdk.core.pipeline.PipelineResult
import org.omniai.sdk.domain.errors.TooManyRequestsError
import org.omniai.sdk.interceptors.ratelimiting.domain.RateLimitResult
import org.omniai.sdk.interceptors.ratelimiting.policy.RateLimitPolicy
import org.omniai.sdk.interceptors.ratelimiting.store.RateLimitStore
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Interceptor responsible for enforcing rate limits across incoming requests.
 * 
 * It evaluates a series of [RateLimitPolicy] implementations injected by the client,
 * extracts dynamic context data, and checks bounds against a given [RateLimitStore].
 * 
 * If any policy results in a throttled state, the interceptor immediately aborts
 * the pipeline and returns a [PipelineResult.Error] containing [TooManyRequestsError].
 * 
 * @property store The storage backend to use for tracking usage.
 * @property policies A list of policies defining how limits are extracted and evaluated.
 */
class RateLimitInterceptor(
    private val store: RateLimitStore,
    private val policies: List<RateLimitPolicy<out Any>>
) : Interceptor {

    override suspend fun handle(context: GatewayContext, chain: InterceptorChain): PipelineResult {
        var maxRetryAfter = Duration.ZERO

        for (policy in policies) {
            // We must use a separate function to capture the generic type safely.
            val retryAfter = evaluatePolicy(policy, context)
            if (retryAfter != null && retryAfter > maxRetryAfter) {
                maxRetryAfter = retryAfter
            }
        }

        if (maxRetryAfter > Duration.ZERO) {
            return PipelineResult.Error(
                TooManyRequestsError(
                    message = "Rate limit exceeded.",
                    retryAfter = maxRetryAfter
                )
            )
        }

        return chain.proceed(context)
    }

    /**
     * Helper function to execute the policy and consume the targets.
     * Returns the required retryAfter Duration if throttled, or null if allowed.
     */
    private suspend fun <T : Any> evaluatePolicy(
        policy: RateLimitPolicy<T>,
        context: GatewayContext
    ): Duration? {
        val extractedData = policy.extract(context) ?: return null
        val targets = policy.evaluate(extractedData)

        var maxRetryAfter = Duration.ZERO

        for (target in targets) {
            // Note: If you have token limits (e.g. RateLimitType.TOKENS), 
            // the weight should be dynamically calculated. For simplicity, we assume weight = 1.
            when (val result = store.consume(target, weight = 1)) {
                is RateLimitResult.Allowed -> { /* Ok, proceed to next target */ }
                is RateLimitResult.Throttled -> {
                    if (result.retryAfter > maxRetryAfter) {
                        maxRetryAfter = result.retryAfter
                    }
                }
            }
        }

        return if (maxRetryAfter > Duration.ZERO) maxRetryAfter else null
    }
}
