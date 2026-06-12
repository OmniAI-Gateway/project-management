package org.omniai.sdk.application.pipeline

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.omniai.sdk.common.Either
import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.common.failure
import org.omniai.sdk.common.success
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent
import kotlinx.coroutines.flow.Flow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GatewayPipelineTest {

    // ── executeUnary — happy path ─────────────────────────────────────────────

    @Test
    fun `executeUnary returns Right with response when dispatcher succeeds`() = runTest {
        val expectedResponse = fakeResponse(id = "unary-ok")
        val dispatcher = FakeSuccessDispatcher(expectedResponse)
        val pipeline = buildPipeline(dispatcher)

        val result = pipeline.executeUnary(fakeContext(mode = RequestMode.UNARY))

        assertIs<Either.Right<CommonResponse>>(result)
        assertEquals(expectedResponse, result.value)
    }

    @Test
    fun `executeUnary delegates request and attributes to dispatcher`() = runTest {
        val dispatcher = FakeSuccessDispatcher()
        val pipeline = buildPipeline(dispatcher)

        pipeline.executeUnary(fakeContext())

        assertTrue(dispatcher.generateCalled, "dispatcher.generate should have been called")
    }

    // ── executeUnary — failure path ───────────────────────────────────────────

    @Test
    fun `executeUnary returns Left when dispatcher fails`() = runTest {
        val error = fakeError("dispatcher blew up")
        val pipeline = buildPipeline(FakeFailureDispatcher(error))

        val result = pipeline.executeUnary(fakeContext())

        assertIs<Either.Left<DomainError>>(result)
        assertEquals(error, result.value)
    }

    // ── executeStream — happy path ────────────────────────────────────────────

    @Test
    fun `executeStream returns Right with flow when dispatcher succeeds`() = runTest {
        val dispatcher = FakeSuccessDispatcher()
        val pipeline = buildPipeline(dispatcher)

        val result = pipeline.executeStream(fakeContext(mode = RequestMode.STREAM))

        assertIs<Either.Right<Flow<CommonResponseEvent>>>(result)
    }

    @Test
    fun `executeStream delegates to dispatcher generateStream`() = runTest {
        val dispatcher = FakeSuccessDispatcher()
        val pipeline = buildPipeline(dispatcher)

        pipeline.executeStream(fakeContext(mode = RequestMode.STREAM))

        assertTrue(dispatcher.generateStreamCalled, "dispatcher.generateStream should have been called")
    }

    // ── executeStream — failure path ──────────────────────────────────────────

    @Test
    fun `executeStream returns Left when dispatcher fails`() = runTest {
        val error = fakeError("stream dispatcher blew up")
        val pipeline = buildPipeline(FakeFailureDispatcher(error))

        val result = pipeline.executeStream(fakeContext(mode = RequestMode.STREAM))

        assertIs<Either.Left<DomainError>>(result)
        assertEquals(error, result.value)
    }

    // ── Short-circuit: interceptor sets result ────────────────────────────────

    @Test
    fun `interceptor can short-circuit by returning PipelineResult without calling chain`() = runTest {
        val shortCircuitResponse = fakeResponse(id = "short-circuit")
        val dispatcher = FakeSuccessDispatcher()  // should NOT be called

        val pipeline = buildPipeline(dispatcher) {
            install(Interceptor { _, _ ->
                // Short-circuit: return directly, do NOT call chain.proceed()
                PipelineResult.Unary(shortCircuitResponse)
            })
        }

        val result = pipeline.executeUnary(fakeContext())

        assertIs<Either.Right<CommonResponse>>(result)
        assertEquals(shortCircuitResponse, result.value)
        // Dispatcher was NOT called because the interceptor short-circuited
        assertTrue(!dispatcher.generateCalled, "Dispatcher should NOT have been called when interceptor short-circuits")
    }

    @Test
    fun `interceptor can modify context before passing to next`() = runTest {
        var contextSeenBySecond: GatewayContext? = null
        val dispatcher = FakeSuccessDispatcher()

        val pipeline = buildPipeline(dispatcher) {
            install(Interceptor { ctx, chain ->
                // Modify context attributes before proceeding
                val modifiedCtx = ctx.copy(
                    attributes = TypedMap().also { it.put("injected", "value") }
                )
                chain.proceed(modifiedCtx)
            })
            install(Interceptor { ctx, chain ->
                contextSeenBySecond = ctx
                chain.proceed(ctx)
            })
        }

        pipeline.executeUnary(fakeContext())

        val injected = contextSeenBySecond?.attributes?.get<String>("injected")
        assertEquals("value", injected, "Second interceptor should see the modified context from first interceptor")
    }

    // ── context.res = PipelineResult.Error ───────────────────────────────────

    @Test
    fun `when context res is Error at end of chain, error is propagated`() = runTest {
        val error = fakeError("pre-set error")
        val pipeline = buildPipeline(FakeSuccessDispatcher())

        val result = pipeline.executeUnary(
            fakeContext(res = PipelineResult.Error(error))
        )

        assertIs<Either.Left<DomainError>>(result)
        assertEquals(error, result.value)
    }

    // ── context.res = PipelineResult.Unary (pre-filled) ──────────────────────

    @Test
    fun `when context res is Unary at end of chain, dispatcher re-runs and overwrites`() = runTest {
        val preFilledResponse = fakeResponse(id = "pre-filled")
        val dispatcherResponse = fakeResponse(id = "from-dispatcher")
        val dispatcher = FakeSuccessDispatcher(dispatcherResponse)
        val pipeline = buildPipeline(dispatcher)

        // A context with res=Unary at end of chain re-dispatches
        val result = pipeline.executeUnary(
            fakeContext(res = PipelineResult.Unary(preFilledResponse))
        )

        // Dispatcher is called and its response wins
        assertIs<Either.Right<CommonResponse>>(result)
        assertEquals(dispatcherResponse, result.value)
    }

    // ── Wrong result type → contract violation ────────────────────────────────

    @Test
    fun `executeUnary returns Left when pipeline produces a Stream result`() = runTest {
        val pipeline = buildPipeline(FakeSuccessDispatcher()) {
            install(Interceptor { _, _ -> PipelineResult.Stream(emptyFlow()) })
        }

        val result = pipeline.executeUnary(fakeContext())

        assertIs<Either.Left<DomainError>>(result, "Stream result on executeUnary should be a contract violation")
        assertTrue(
            result.value.message.contains("Contract violation"),
            "Error message should mention contract violation, got: ${result.value.message}"
        )
    }

    @Test
    fun `executeStream returns Left when pipeline produces a Unary result`() = runTest {
        val pipeline = buildPipeline(FakeSuccessDispatcher()) {
            install(Interceptor { _, _ -> PipelineResult.Unary(fakeResponse()) })
        }

        val result = pipeline.executeStream(fakeContext(mode = RequestMode.STREAM))

        assertIs<Either.Left<DomainError>>(result, "Unary result on executeStream should be a contract violation")
        assertTrue(
            result.value.message.contains("Contract violation"),
            "Error message should mention contract violation, got: ${result.value.message}"
        )
    }

    // ── No interceptors: goes straight to dispatcher ──────────────────────────

    @Test
    fun `pipeline with no interceptors calls dispatcher directly`() = runTest {
        val dispatcher = FakeSuccessDispatcher()
        val pipeline = buildPipeline(dispatcher) { /* no interceptors */ }

        pipeline.executeUnary(fakeContext())

        assertTrue(dispatcher.generateCalled)
    }
}
