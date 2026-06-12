package org.omniai.sdk.application.pipeline

import kotlinx.coroutines.flow.emptyFlow
import org.omniai.sdk.common.Either
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.errors.UnknownDomainError
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent
import kotlinx.coroutines.flow.Flow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PipelineResultTest {

    // ─── fold() ───────────────────────────────────────────────────────────────

    @Test
    fun `fold calls onUnary branch for Unary result`() {
        val result: PipelineResult = PipelineResult.Unary(fakeResponse())
        val branch = result.fold(
            onUnary = { "unary" },
            onStream = { "stream" },
            onError = { "error" },
            onNothing = { "nothing" }
        )
        assertEquals("unary", branch)
    }

    @Test
    fun `fold calls onStream branch for Stream result`() {
        val result: PipelineResult = PipelineResult.Stream(emptyFlow())
        val branch = result.fold(
            onUnary = { "unary" },
            onStream = { "stream" },
            onError = { "error" },
            onNothing = { "nothing" }
        )
        assertEquals("stream", branch)
    }

    @Test
    fun `fold calls onError branch for Error result`() {
        val result: PipelineResult = PipelineResult.Error(fakeError())
        val branch = result.fold(
            onUnary = { "unary" },
            onStream = { "stream" },
            onError = { "error" },
            onNothing = { "nothing" }
        )
        assertEquals("error", branch)
    }

    @Test
    fun `fold calls onNothing branch for NoResult`() {
        val result: PipelineResult = PipelineResult.NoResult
        val branch = result.fold(
            onUnary = { "unary" },
            onStream = { "stream" },
            onError = { "error" },
            onNothing = { "nothing" }
        )
        assertEquals("nothing", branch)
    }

    // ─── requireUnaryResponse() ───────────────────────────────────────────────

    @Test
    fun `requireUnaryResponse returns Right for Unary result`() {
        val response = fakeResponse(id = "u-1")
        val result = PipelineResult.Unary(response).requireUnaryResponse()
        assertIs<Either.Right<CommonResponse>>(result)
        assertEquals(response, result.value)
    }

    @Test
    fun `requireUnaryResponse returns Left for Error result`() {
        val error = fakeError("boom")
        val result = PipelineResult.Error(error).requireUnaryResponse()
        assertIs<Either.Left<DomainError>>(result)
        assertEquals(error, result.value)
    }

    @Test
    fun `requireUnaryResponse returns Left contract violation for Stream result`() {
        val result = PipelineResult.Stream(emptyFlow()).requireUnaryResponse()
        assertIs<Either.Left<DomainError>>(result)
        assertTrue(
            result.value.message.contains("Contract violation"),
            "Expected contract violation message, got: ${result.value.message}"
        )
    }

    @Test
    fun `requireUnaryResponse returns Left contract violation for NoResult`() {
        val result = PipelineResult.NoResult.requireUnaryResponse()
        assertIs<Either.Left<DomainError>>(result)
        assertTrue(result.value.message.contains("Contract violation"))
    }

    // ─── requireStreamEvents() ────────────────────────────────────────────────

    @Test
    fun `requireStreamEvents returns Right for Stream result`() {
        val flow = emptyFlow<CommonResponseEvent>()
        val result = PipelineResult.Stream(flow).requireStreamEvents()
        assertIs<Either.Right<Flow<CommonResponseEvent>>>(result)
    }

    @Test
    fun `requireStreamEvents returns Left for Error result`() {
        val error = fakeError("stream-err")
        val result = PipelineResult.Error(error).requireStreamEvents()
        assertIs<Either.Left<DomainError>>(result)
        assertEquals(error, result.value)
    }

    @Test
    fun `requireStreamEvents returns Left contract violation for Unary result`() {
        val result = PipelineResult.Unary(fakeResponse()).requireStreamEvents()
        assertIs<Either.Left<DomainError>>(result)
        assertTrue(
            result.value.message.contains("Contract violation"),
            "Expected contract violation message, got: ${result.value.message}"
        )
    }

    @Test
    fun `requireStreamEvents returns Left contract violation for NoResult`() {
        val result = PipelineResult.NoResult.requireStreamEvents()
        assertIs<Either.Left<DomainError>>(result)
        assertTrue(result.value.message.contains("Contract violation"))
    }

    // ─── UnknownDomainError used in violations ────────────────────────────────

    @Test
    fun `contract violation for requireUnaryResponse is an UnknownDomainError`() {
        val result = PipelineResult.NoResult.requireUnaryResponse()
        assertIs<Either.Left<DomainError>>(result)
        assertIs<UnknownDomainError>(result.value)
    }

    @Test
    fun `contract violation for requireStreamEvents is an UnknownDomainError`() {
        val result = PipelineResult.NoResult.requireStreamEvents()
        assertIs<Either.Left<DomainError>>(result)
        assertIs<UnknownDomainError>(result.value)
    }
}
