package org.omniai.sdk.application.pipeline

import kotlinx.coroutines.test.runTest
import org.omniai.sdk.common.Either
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.responses.CommonResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GatewayPipelineChainTest {

    // ── No interceptors: goes straight to dispatcher ──────────────────────────

    @Test
    fun `chain with no interceptors calls dispatcher generate for UNARY mode`() = runTest {
        val dispatcher = FakeSuccessDispatcher(fakeResponse(id = "direct"))
        val chain = GatewayPipelineChain(emptyList(), dispatcher, 0)

        val result = chain.proceed(fakeContext(mode = RequestMode.UNARY))

        assertIs<PipelineResult.Unary>(result)
        assertEquals("direct", result.response.id)
    }

    @Test
    fun `chain with no interceptors calls dispatcher generateStream for STREAM mode`() = runTest {
        val dispatcher = FakeSuccessDispatcher()
        val chain = GatewayPipelineChain(emptyList(), dispatcher, 0)

        val result = chain.proceed(fakeContext(mode = RequestMode.STREAM))

        assertIs<PipelineResult.Stream>(result)
        assertTrue(dispatcher.generateStreamCalled)
    }

    // ── Dispatcher failure propagated ─────────────────────────────────────────

    @Test
    fun `chain returns Error when dispatcher generate fails`() = runTest {
        val error = fakeError("dispatcher error")
        val chain = GatewayPipelineChain(emptyList(), FakeFailureDispatcher(error), 0)

        val result = chain.proceed(fakeContext(mode = RequestMode.UNARY))

        assertIs<PipelineResult.Error>(result)
        assertEquals(error, result.error)
    }

    @Test
    fun `chain returns Error when dispatcher generateStream fails`() = runTest {
        val error = fakeError("stream error")
        val chain = GatewayPipelineChain(emptyList(), FakeFailureDispatcher(error), 0)

        val result = chain.proceed(fakeContext(mode = RequestMode.STREAM))

        assertIs<PipelineResult.Error>(result)
        assertEquals(error, result.error)
    }

    // ── ctx.res already set ───────────────────────────────────────────────────

    @Test
    fun `chain returns Error immediately when context res is Error and no interceptors remain`() = runTest {
        val error = fakeError("pre-existing error")
        val dispatcher = FakeSuccessDispatcher() // must NOT be called
        val chain = GatewayPipelineChain(emptyList(), dispatcher, 0)

        val result = chain.proceed(fakeContext(res = PipelineResult.Error(error)))

        assertIs<PipelineResult.Error>(result)
        assertEquals(error, result.error)
        assertTrue(!dispatcher.generateCalled, "Dispatcher must not be called when result is already an Error")
    }

    @Test
    fun `chain re-dispatches when context res is Unary and no interceptors remain`() = runTest {
        val dispatcherResponse = fakeResponse(id = "from-dispatcher")
        val dispatcher = FakeSuccessDispatcher(dispatcherResponse)
        val chain = GatewayPipelineChain(emptyList(), dispatcher, 0)

        val result = chain.proceed(fakeContext(res = PipelineResult.Unary(fakeResponse(id = "stale"))))

        assertIs<PipelineResult.Unary>(result)
        assertEquals("from-dispatcher", result.response.id)
    }

    @Test
    fun `chain re-dispatches when context res is Stream and no interceptors remain`() = runTest {
        val dispatcher = FakeSuccessDispatcher()
        val chain = GatewayPipelineChain(emptyList(), dispatcher, 0)

        val result = chain.proceed(
            fakeContext(
                mode = RequestMode.STREAM,
                res = PipelineResult.Stream(kotlinx.coroutines.flow.emptyFlow())
            )
        )

        assertIs<PipelineResult.Stream>(result)
        assertTrue(dispatcher.generateStreamCalled)
    }

    // ── Interceptor chain order ───────────────────────────────────────────────

    @Test
    fun `interceptors are invoked in order and each sees the incremented index`() = runTest {
        val log = mutableListOf<Int>()

        val interceptors = listOf(
            Interceptor { ctx, chain -> log += 0; chain.proceed(ctx) },
            Interceptor { ctx, chain -> log += 1; chain.proceed(ctx) },
            Interceptor { ctx, chain -> log += 2; chain.proceed(ctx) },
        )

        val chain = GatewayPipelineChain(interceptors, FakeSuccessDispatcher(), 0)
        chain.proceed(fakeContext())

        assertEquals(listOf(0, 1, 2), log)
    }

    @Test
    fun `interceptor at end of list delegates to dispatcher`() = runTest {
        val dispatcher = FakeSuccessDispatcher()
        val interceptors = listOf(
            Interceptor { ctx, chain -> chain.proceed(ctx) }
        )
        val chain = GatewayPipelineChain(interceptors, dispatcher, 0)

        chain.proceed(fakeContext())

        assertTrue(dispatcher.generateCalled)
    }
}
