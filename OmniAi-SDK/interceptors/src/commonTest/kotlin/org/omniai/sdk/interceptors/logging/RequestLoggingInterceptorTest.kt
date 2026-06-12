package org.omniai.sdk.interceptors.logging

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.omniai.sdk.application.pipeline.PipelineResult
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.responses.CommonResponseEvent
import org.omniai.sdk.domain.responses.ResponseStarted
import org.omniai.sdk.interceptors.helper.StaticChain
import org.omniai.sdk.interceptors.helper.fakeContext
import org.omniai.sdk.interceptors.helper.fakeError
import org.omniai.sdk.interceptors.helper.fakeResponse
import org.omniai.sdk.interceptors.logger.GatewayLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RequestLoggingInterceptorTest {

    // ─── Capturing logger ─────────────────────────────────────────────────────

    private class CapturingLogger : GatewayLogger {
        val infos = mutableListOf<String>()
        val warns = mutableListOf<String>()
        val errors = mutableListOf<String>()

        override fun info(message: String, vararg args: Any?) {
            infos += interpolate(message, args)
        }

        override fun warn(message: String, vararg args: Any?) {
            warns += interpolate(message, args)
        }

        override fun error(message: String, vararg args: Any?) {
            errors += interpolate(message, args)
        }

        private fun interpolate(message: String, args: Array<out Any?>): String {
            var result = message
            args.forEach { arg ->
                result = result.replaceFirst("{}", arg?.toString() ?: "null")
            }
            return result
        }
    }

    // ─── Unary: info logged with provider and model ───────────────────────────

    @Test
    fun `unary request logs info with provider and model`() = runTest {
        val logger = CapturingLogger()
        val interceptor = RequestLoggingInterceptor(logger)
        val response = fakeResponse(provider = Provider.OPENAI, model = "gpt-4o")

        interceptor.handle(fakeContext(Provider.OPENAI, "gpt-4o"), StaticChain(PipelineResult.Unary(response)))

        assertTrue(logger.infos.any { it.contains("openai") && it.contains("gpt-4o") },
            "Request info log should contain provider and model. Got: ${logger.infos}")
    }

    @Test
    fun `unary response logs info with response provider and model`() = runTest {
        val logger = CapturingLogger()
        val interceptor = RequestLoggingInterceptor(logger)
        val response = fakeResponse(provider = Provider.ANTHROPIC, model = "claude-3")

        interceptor.handle(fakeContext(), StaticChain(PipelineResult.Unary(response)))

        assertEquals(2, logger.infos.size, "Should have 2 info logs (request + response)")
        assertTrue(logger.infos[1].contains("anthropic") && logger.infos[1].contains("claude-3"),
            "Response log should contain response provider and model. Got: ${logger.infos[1]}")
    }

    @Test
    fun `unary result is passed through unchanged`() = runTest {
        val response = fakeResponse(id = "resp-test")
        val interceptor = RequestLoggingInterceptor(CapturingLogger())

        val result = interceptor.handle(fakeContext(), StaticChain(PipelineResult.Unary(response)))

        assertIs<PipelineResult.Unary>(result)
        assertEquals("resp-test", result.response.id)
    }

    // ─── Error: warn logged ───────────────────────────────────────────────────

    @Test
    fun `error result logs a warning with the error message`() = runTest {
        val logger = CapturingLogger()
        val interceptor = RequestLoggingInterceptor(logger)
        val error = fakeError("something went wrong")

        interceptor.handle(fakeContext(), StaticChain(PipelineResult.Error(error)))

        assertTrue(logger.warns.any { it.contains("something went wrong") },
            "Warn log should contain error message. Got: ${logger.warns}")
    }

    @Test
    fun `error result is passed through unchanged`() = runTest {
        val error = fakeError("pass-through check")
        val interceptor = RequestLoggingInterceptor(CapturingLogger())

        val result = interceptor.handle(fakeContext(), StaticChain(PipelineResult.Error(error)))

        assertIs<PipelineResult.Error>(result)
        assertEquals(error, result.error)
    }

    // ─── Stream: completion logging ────────────────────────────────────────────

    @Test
    fun `stream completion logs info`() = runTest {
        val logger = CapturingLogger()
        val interceptor = RequestLoggingInterceptor(logger)

        val event = ResponseStarted(
            provider = Provider.OPENAI, id = "s1",
            model = Model("gpt-4o"), sequence = 0
        )
        val chain = StaticChain(PipelineResult.Stream(flow { emit(event) }))

        val result = interceptor.handle(fakeContext(), chain)
        assertIs<PipelineResult.Stream>(result)

        // Collect to trigger onCompletion
        result.eventFlow.toList()

        assertTrue(logger.infos.any { it.contains("stream") },
            "Completed stream should log an info. Got: ${logger.infos}")
    }

    @Test
    fun `stream failure logs error`() = runTest {
        val logger = CapturingLogger()
        val interceptor = RequestLoggingInterceptor(logger)

        val chain = StaticChain(PipelineResult.Stream(flow<CommonResponseEvent> {
            throw IllegalStateException("stream exploded")
        }))

        val result = interceptor.handle(fakeContext(), chain)
        assertIs<PipelineResult.Stream>(result)

        try { result.eventFlow.toList() } catch (_: IllegalStateException) {}

        assertTrue(logger.errors.any { it.contains("stream exploded") },
            "Failed stream should log an error. Got: ${logger.errors}")
    }

    // ─── NoResult passthrough ─────────────────────────────────────────────────

    @Test
    fun `NoResult is passed through without any logs beyond the initial request log`() = runTest {
        val logger = CapturingLogger()
        val interceptor = RequestLoggingInterceptor(logger)

        val result = interceptor.handle(fakeContext(), StaticChain(PipelineResult.NoResult))

        assertIs<PipelineResult.NoResult>(result)
        assertEquals(1, logger.infos.size, "Only the request info should have been logged")
        assertTrue(logger.warns.isEmpty())
        assertTrue(logger.errors.isEmpty())
    }

    // ─── No-op logger (default) ───────────────────────────────────────────────

    @Test
    fun `default NoOp logger does not throw`() = runTest {
        val interceptor = RequestLoggingInterceptor() // uses NoOpGatewayLogger
        val result = interceptor.handle(fakeContext(), StaticChain(PipelineResult.Unary(fakeResponse())))
        assertIs<PipelineResult.Unary>(result)
    }
}
