package org.omniai.sdk.interceptors.tracing

import kotlinx.coroutines.test.runTest
import org.omniai.sdk.application.pipeline.PipelineResult
import org.omniai.sdk.interceptors.helper.StaticChain
import org.omniai.sdk.interceptors.helper.fakeContext
import org.omniai.sdk.interceptors.helper.fakeError
import org.omniai.sdk.interceptors.helper.fakeResponse
import org.omniai.sdk.interceptors.metrics.Tracer
import org.omniai.sdk.interceptors.metrics.TracingInterceptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TracingInterceptorTest {
    // ─── Capturing tracer ─────────────────────────────────────────────────────

    private class CapturingTracer : Tracer {
        val spans = mutableListOf<String>()

        override suspend fun <T> withSpan(
            spanName: String,
            block: suspend () -> T,
        ): T {
            spans += spanName
            return block()
        }
    }

    // ─── Span is always opened ────────────────────────────────────────────────

    @Test
    fun `span is created with correct name for every request`() =
        runTest {
            val tracer = CapturingTracer()
            val interceptor = TracingInterceptor(tracer)

            interceptor.handle(fakeContext(), StaticChain(PipelineResult.Unary(fakeResponse())))

            assertEquals(1, tracer.spans.size)
            assertEquals("gateway.request.process", tracer.spans.single())
        }

    @Test
    fun `span is created for each request independently`() =
        runTest {
            val tracer = CapturingTracer()
            val interceptor = TracingInterceptor(tracer)
            val chain = StaticChain(PipelineResult.Unary(fakeResponse()))

            repeat(3) { interceptor.handle(fakeContext(), chain) }

            assertEquals(3, tracer.spans.size)
        }

    // ─── Result passthrough ───────────────────────────────────────────────────

    @Test
    fun `unary result from chain is returned as-is`() =
        runTest {
            val tracer = CapturingTracer()
            val interceptor = TracingInterceptor(tracer)
            val response = fakeResponse(id = "trace-resp")

            val result = interceptor.handle(fakeContext(), StaticChain(PipelineResult.Unary(response)))

            assertIs<PipelineResult.Unary>(result)
            assertEquals("trace-resp", result.response.id)
        }

    @Test
    fun `error result from chain is returned as-is`() =
        runTest {
            val tracer = CapturingTracer()
            val interceptor = TracingInterceptor(tracer)
            val error = fakeError("trace error")

            val result = interceptor.handle(fakeContext(), StaticChain(PipelineResult.Error(error)))

            assertIs<PipelineResult.Error>(result)
            assertEquals(error, result.error)
        }

    @Test
    fun `NoResult from chain is returned as-is`() =
        runTest {
            val tracer = CapturingTracer()
            val interceptor = TracingInterceptor(tracer)

            val result = interceptor.handle(fakeContext(), StaticChain(PipelineResult.NoResult))

            assertIs<PipelineResult.NoResult>(result)
        }

    // ─── Span still recorded even on error ────────────────────────────────────

    @Test
    fun `span is recorded even when chain returns an error`() =
        runTest {
            val tracer = CapturingTracer()
            val interceptor = TracingInterceptor(tracer)

            interceptor.handle(fakeContext(), StaticChain(PipelineResult.Error(fakeError())))

            assertEquals(1, tracer.spans.size)
            assertEquals("gateway.request.process", tracer.spans.single())
        }
}
