package org.omniai.sdk.interceptors.circuitBreaker

import kotlinx.coroutines.test.runTest
import org.omniai.sdk.application.pipeline.PipelineResult
import org.omniai.sdk.common.key
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.errors.ApiDownError
import org.omniai.sdk.interceptors.helper.CapturingChain
import org.omniai.sdk.interceptors.helper.FakeOutbound
import org.omniai.sdk.interceptors.helper.StaticChain
import org.omniai.sdk.interceptors.helper.fakeContext
import org.omniai.sdk.interceptors.helper.fakeError
import org.omniai.sdk.interceptors.helper.fakeResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CircuitBreakerInterceptorTest {
    private val deniedKey = key<Set<String>>("deniedOutbounds")
    private val config = CircuitBreakerConfig(failureThreshold = 3)

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun outbound(
        provider: Provider = Provider.OPENAI,
        model: String = "gpt-4o",
    ) = FakeOutbound(provider, model)

    private fun interceptor(
        store: CircuitBreakerStore = InMemoryCircuitBreakerStore(),
        outbounds: List<FakeOutbound> = listOf(outbound()),
        cfg: CircuitBreakerConfig = config,
    ) = CircuitBreakerInterceptor(
        store = store,
        config = cfg,
        deniedOutboundsKey = deniedKey,
        outbounds = outbounds,
    )

    // ─── CLOSED state: requests pass through ─────────────────────────────────

    @Test
    fun `CLOSED circuit passes request to next chain`() =
        runTest {
            val chain = StaticChain(PipelineResult.Unary(fakeResponse()))
            val cb = interceptor()

            val result = cb.handle(fakeContext(), chain)

            assertIs<PipelineResult.Unary>(result)
            assertEquals(1, chain.callCount)
        }

    @Test
    fun `CLOSED circuit records success and stays CLOSED on success`() =
        runTest {
            val store = InMemoryCircuitBreakerStore()
            val cb = interceptor(store = store)

            cb.handle(fakeContext(), StaticChain(PipelineResult.Unary(fakeResponse())))

            assertEquals(CircuitState.CLOSED, store.getState("openai:gpt-4o"))
        }

    // ─── OPEN state: fast-fail ────────────────────────────────────────────────

    @Test
    fun `OPEN circuit immediately returns Error without calling chain`() =
        runTest {
            val store = InMemoryCircuitBreakerStore()
            store.transitionState("openai:gpt-4o", CircuitState.OPEN)
            val chain = StaticChain(PipelineResult.Unary(fakeResponse()))
            val cb = interceptor(store = store)

            val result = cb.handle(fakeContext(), chain)

            assertIs<PipelineResult.Error>(result)
            assertIs<ApiDownError>(result.error)
            assertEquals(0, chain.callCount, "Chain must NOT be called when circuit is OPEN")
        }

    @Test
    fun `OPEN circuit adds outbound to denied set in context attributes`() =
        runTest {
            val store = InMemoryCircuitBreakerStore()
            store.transitionState("openai:gpt-4o", CircuitState.OPEN)
            val capturing = CapturingChain()
            val cb = interceptor(store = store)

            cb.handle(fakeContext(), capturing)

            // Context is NOT forwarded (chain never called), but attributes are mutated directly
            val ctx = fakeContext()
            cb.handle(ctx, capturing)
            val denied = ctx.attributes[deniedKey]
            assertTrue(denied != null && denied.contains("openai:gpt-4o"))
        }

    // ─── Failure threshold triggers OPEN ─────────────────────────────────────

    @Test
    fun `failures at threshold transition circuit to OPEN`() =
        runTest {
            val store = InMemoryCircuitBreakerStore()
            val cb = interceptor(store = store, cfg = CircuitBreakerConfig(failureThreshold = 2))
            val errorChain = StaticChain(PipelineResult.Error(fakeError()))

            repeat(2) { cb.handle(fakeContext(), errorChain) }

            assertEquals(CircuitState.OPEN, store.getState("openai:gpt-4o"))
        }

    @Test
    fun `failures below threshold do not open circuit`() =
        runTest {
            val store = InMemoryCircuitBreakerStore()
            val cb = interceptor(store = store, cfg = CircuitBreakerConfig(failureThreshold = 5))
            val errorChain = StaticChain(PipelineResult.Error(fakeError()))

            repeat(4) { cb.handle(fakeContext(), errorChain) }

            assertEquals(CircuitState.CLOSED, store.getState("openai:gpt-4o"))
        }

    // ─── HALF_OPEN: success closes the circuit ────────────────────────────────

    @Test
    fun `success in HALF_OPEN transitions circuit to CLOSED`() =
        runTest {
            val store = InMemoryCircuitBreakerStore()
            store.transitionState("openai:gpt-4o", CircuitState.HALF_OPEN)
            val cb = interceptor(store = store)

            cb.handle(fakeContext(), StaticChain(PipelineResult.Unary(fakeResponse())))

            assertEquals(CircuitState.CLOSED, store.getState("openai:gpt-4o"))
        }

    // ─── No matching outbound: passes through ────────────────────────────────

    @Test
    fun `no matching outbound passes request through without state changes`() =
        runTest {
            val chain = StaticChain(PipelineResult.Unary(fakeResponse()))
            // Outbound registered for anthropic, but request is for openai
            val cb = interceptor(outbounds = listOf(FakeOutbound(Provider.ANTHROPIC, "claude-3")))

            val result = cb.handle(fakeContext(provider = Provider.OPENAI, model = "gpt-4o"), chain)

            assertIs<PipelineResult.Unary>(result)
            assertEquals(1, chain.callCount)
        }

    // ─── InMemoryCircuitBreakerStore standalone ───────────────────────────────

    @Test
    fun `store starts CLOSED with zero failures`() =
        runTest {
            val store = InMemoryCircuitBreakerStore()
            assertEquals(CircuitState.CLOSED, store.getState("any"))
            assertEquals(0, store.getFailures("any"))
        }

    @Test
    fun `recordFailure increments failure count`() =
        runTest {
            val store = InMemoryCircuitBreakerStore()
            store.recordFailure("x")
            store.recordFailure("x")
            assertEquals(2, store.getFailures("x"))
        }

    @Test
    fun `recordSuccess resets failure count`() =
        runTest {
            val store = InMemoryCircuitBreakerStore()
            repeat(3) { store.recordFailure("x") }
            store.recordSuccess("x")
            assertEquals(0, store.getFailures("x"))
        }

    @Test
    fun `transitionState to CLOSED resets failures`() =
        runTest {
            val store = InMemoryCircuitBreakerStore()
            repeat(5) { store.recordFailure("x") }
            store.transitionState("x", CircuitState.CLOSED)
            assertEquals(0, store.getFailures("x"))
            assertEquals(CircuitState.CLOSED, store.getState("x"))
        }

    @Test
    fun `different outbound IDs are tracked independently`() =
        runTest {
            val store = InMemoryCircuitBreakerStore()
            store.recordFailure("a")
            store.recordFailure("a")
            store.transitionState("a", CircuitState.OPEN)

            assertEquals(CircuitState.OPEN, store.getState("a"))
            assertEquals(CircuitState.CLOSED, store.getState("b"))
            assertEquals(0, store.getFailures("b"))
        }
}
