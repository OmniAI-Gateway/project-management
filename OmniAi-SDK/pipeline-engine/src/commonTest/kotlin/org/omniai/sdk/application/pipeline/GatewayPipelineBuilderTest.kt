package org.omniai.sdk.application.pipeline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class GatewayPipelineBuilderTest {

    // ── build() ───────────────────────────────────────────────────────────────

    @Test
    fun `build throws when dispatcher was not installed`() {
        val builder = GatewayPipelineBuilder()

        val ex = assertFailsWith<IllegalStateException> {
            builder.build()
        }
        assertTrue(
            ex.message!!.contains("Terminal dispatcher obrigatório"),
            "Expected message about missing dispatcher, got: ${ex.message}"
        )
    }

    @Test
    fun `build succeeds when dispatcher is installed`() {
        val pipeline = buildPipeline()
        assertTrue(pipeline is GatewayPipeline)
    }

    // ── install / intercept ───────────────────────────────────────────────────

    @Test
    fun `install adds interceptor that is called during execution`() = runTest {
        var called = false
        val dispatcher = FakeSuccessDispatcher()

        val pipeline = buildPipeline(dispatcher) {
            install(Interceptor { ctx, chain ->
                called = true
                chain.proceed(ctx)
            })
        }

        pipeline.executeUnary(fakeContext())

        assertTrue(called, "Interceptor installed via install() should have been called")
    }

    @Test
    fun `intercept lambda is called during execution`() = runTest {
        var called = false
        val dispatcher = FakeSuccessDispatcher()

        val pipeline = buildPipeline(dispatcher) {
            intercept { ctx, chain ->
                called = true
                chain.proceed(ctx)
            }
        }

        pipeline.executeUnary(fakeContext())

        assertTrue(called, "Interceptor registered via intercept{} should have been called")
    }

    @Test
    fun `provider adds interceptor that is called during execution`() = runTest {
        var called = false
        val dispatcher = FakeSuccessDispatcher()

        val pipeline = buildPipeline(dispatcher) {
            provider(Interceptor { ctx, chain ->
                called = true
                chain.proceed(ctx)
            })
        }

        pipeline.executeUnary(fakeContext())

        assertTrue(called, "Interceptor registered via provider() should have been called")
    }

    @Test
    fun `multiple interceptors are all invoked`() = runTest {
        val callLog = mutableListOf<Int>()
        val dispatcher = FakeSuccessDispatcher()

        val pipeline = buildPipeline(dispatcher) {
            install(Interceptor { ctx, chain -> callLog += 1; chain.proceed(ctx) })
            install(Interceptor { ctx, chain -> callLog += 2; chain.proceed(ctx) })
            install(Interceptor { ctx, chain -> callLog += 3; chain.proceed(ctx) })
        }

        pipeline.executeUnary(fakeContext())

        assertEquals(listOf(1, 2, 3), callLog, "All interceptors should be called in insertion order, got: $callLog")
    }
}