package org.omniai.sdk.interceptors.ratelimiting

import kotlinx.coroutines.test.runTest
import org.omniai.sdk.application.pipeline.GatewayContext
import org.omniai.sdk.application.pipeline.PipelineResult
import org.omniai.sdk.domain.errors.TooManyRequestsError
import org.omniai.sdk.interceptors.helper.StaticChain
import org.omniai.sdk.interceptors.helper.fakeContext
import org.omniai.sdk.interceptors.helper.fakeResponse
import org.omniai.sdk.interceptors.ratelimiting.domain.RateLimitDefinition
import org.omniai.sdk.interceptors.ratelimiting.domain.RateLimitResult
import org.omniai.sdk.interceptors.ratelimiting.domain.RateLimitTarget
import org.omniai.sdk.interceptors.ratelimiting.domain.RateLimitType
import org.omniai.sdk.interceptors.ratelimiting.domain.RateLimitWindow
import org.omniai.sdk.interceptors.ratelimiting.policy.RateLimitPolicy
import org.omniai.sdk.interceptors.ratelimiting.store.RateLimitStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RateLimitInterceptorTest {

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun window() = RateLimitWindow(60.seconds)
    private fun definition(limit: Int = 100) =
        RateLimitDefinition(limit = limit, type = RateLimitType.REQUESTS, window = window())

    private fun target(key: String = "user:123") = RateLimitTarget(key = key, definition = definition())

    /** Policy that always extracts a String key and returns the given targets. */
    private fun policyAllowed(targets: List<RateLimitTarget> = listOf(target())) =
        object : RateLimitPolicy<String> {
            override suspend fun extract(context: GatewayContext) = "tenant-a"
            override suspend fun evaluate(data: String) = targets
        }

    /** Policy that always returns null from extract() — should be skipped. */
    private val skippedPolicy = object : RateLimitPolicy<String> {
        override suspend fun extract(context: GatewayContext): String? = null
        override suspend fun evaluate(data: String) = error("should not be called")
    }

    /** Store that always allows. */
    private val alwaysAllow = object : RateLimitStore {
        override suspend fun consume(target: RateLimitTarget, weight: Int) =
            RateLimitResult.Allowed(remaining = 99)
    }

    /** Store that always throttles with the given retryAfter. */
    private fun alwaysThrottle(retryAfter: Duration = 5.seconds) = object : RateLimitStore {
        override suspend fun consume(target: RateLimitTarget, weight: Int) =
            RateLimitResult.Throttled(retryAfter = retryAfter)
    }

    // ─── Allowed requests ─────────────────────────────────────────────────────

    @Test
    fun `allowed request passes through to chain`() = runTest {
        val chain = StaticChain(PipelineResult.Unary(fakeResponse()))
        val interceptor = RateLimitInterceptor(
            store = alwaysAllow,
            policies = listOf(policyAllowed())
        )

        val result = interceptor.handle(fakeContext(), chain)

        assertIs<PipelineResult.Unary>(result)
        assertEquals(1, chain.callCount)
    }

    @Test
    fun `no policies request always passes through`() = runTest {
        val chain = StaticChain(PipelineResult.Unary(fakeResponse()))
        val interceptor = RateLimitInterceptor(store = alwaysAllow, policies = emptyList())

        val result = interceptor.handle(fakeContext(), chain)

        assertIs<PipelineResult.Unary>(result)
        assertEquals(1, chain.callCount)
    }

    @Test
    fun `policy with null extract is skipped and request passes through`() = runTest {
        val chain = StaticChain(PipelineResult.Unary(fakeResponse()))
        val interceptor = RateLimitInterceptor(
            store = alwaysAllow,
            policies = listOf(skippedPolicy)
        )

        val result = interceptor.handle(fakeContext(), chain)

        assertIs<PipelineResult.Unary>(result)
    }

    // ─── Throttled requests ───────────────────────────────────────────────────

    @Test
    fun `throttled request returns Error with TooManyRequestsError`() = runTest {
        val chain = StaticChain(PipelineResult.Unary(fakeResponse()))
        val interceptor = RateLimitInterceptor(
            store = alwaysThrottle(10.seconds),
            policies = listOf(policyAllowed())
        )

        val result = interceptor.handle(fakeContext(), chain)

        assertIs<PipelineResult.Error>(result)
        assertIs<TooManyRequestsError>(result.error)
        assertEquals(0, chain.callCount, "Chain must NOT be called when rate limited")
    }

    @Test
    fun `throttled request carries the retryAfter duration from the store`() = runTest {
        val interceptor = RateLimitInterceptor(
            store = alwaysThrottle(42.seconds),
            policies = listOf(policyAllowed())
        )

        val result = interceptor.handle(fakeContext(), StaticChain(PipelineResult.Unary(fakeResponse())))

        assertIs<PipelineResult.Error>(result)
        val error = result.error as TooManyRequestsError
        assertEquals(42.seconds, error.retryAfter)
    }

    @Test
    fun `when multiple policies throttle, the largest retryAfter wins`() = runTest {
        val smallDelay = 5.seconds
        val bigDelay = 30.seconds

        val interceptor = RateLimitInterceptor(
            store = object : RateLimitStore {
                var callIdx = 0
                override suspend fun consume(target: RateLimitTarget, weight: Int): RateLimitResult {
                    return when (callIdx++) {
                        0 -> RateLimitResult.Throttled(smallDelay)
                        else -> RateLimitResult.Throttled(bigDelay)
                    }
                }
            },
            policies = listOf(
                policyAllowed(listOf(target("a"))),
                policyAllowed(listOf(target("b")))
            )
        )

        val result = interceptor.handle(fakeContext(), StaticChain(PipelineResult.Unary(fakeResponse())))

        assertIs<PipelineResult.Error>(result)
        assertEquals(bigDelay, (result.error as TooManyRequestsError).retryAfter)
    }

    // ─── Mixed policies ───────────────────────────────────────────────────────

    @Test
    fun `one allowed policy and one throttled policy results in throttle`() = runTest {
        val interceptor = RateLimitInterceptor(
            store = object : RateLimitStore {
                var callCount = 0
                override suspend fun consume(target: RateLimitTarget, weight: Int): RateLimitResult {
                    return if (callCount++ == 0) RateLimitResult.Allowed(10)
                    else RateLimitResult.Throttled(3.seconds)
                }
            },
            policies = listOf(
                policyAllowed(listOf(target("a"))),
                policyAllowed(listOf(target("b")))
            )
        )

        val result = interceptor.handle(fakeContext(), StaticChain(PipelineResult.Unary(fakeResponse())))

        assertIs<PipelineResult.Error>(result)
        assertIs<TooManyRequestsError>(result.error)
    }

    @Test
    fun `skipped policy mixed with throttled policy still throttles`() = runTest {
        val interceptor = RateLimitInterceptor(
            store = alwaysThrottle(7.seconds),
            policies = listOf(skippedPolicy, policyAllowed())
        )

        val result = interceptor.handle(fakeContext(), StaticChain(PipelineResult.Unary(fakeResponse())))

        assertIs<PipelineResult.Error>(result)
    }
}
