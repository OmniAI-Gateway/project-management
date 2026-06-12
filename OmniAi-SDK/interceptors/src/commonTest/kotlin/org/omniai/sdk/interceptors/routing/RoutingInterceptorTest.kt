package org.omniai.sdk.interceptors.routing

import kotlinx.coroutines.test.runTest
import org.omniai.sdk.application.pipeline.PipelineResult
import org.omniai.sdk.application.pipeline.RequestMode
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.errors.UnknownDomainError
import org.omniai.sdk.interceptors.helper.CapturingChain
import org.omniai.sdk.interceptors.helper.FakeOutbound
import org.omniai.sdk.interceptors.helper.StaticChain
import org.omniai.sdk.interceptors.helper.fakeContext
import org.omniai.sdk.interceptors.helper.fakeResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RoutingInterceptorTest {

    // ─── Empty outbound list ──────────────────────────────────────────────────

    @Test
    fun `empty outbounds returns Error with descriptive message`() = runTest {
        val interceptor = RoutingInterceptor(emptyList())
        val chain = StaticChain(PipelineResult.Unary(fakeResponse()))

        val result = interceptor.handle(fakeContext(), chain)

        assertIs<PipelineResult.Error>(result)
        assertIs<UnknownDomainError>(result.error)
        assertTrue(result.error.message.contains("outbound", ignoreCase = true),
            "Error message should mention outbounds. Got: ${result.error.message}")
        assertEquals(0, chain.callCount, "Chain must NOT be called when no outbounds are available")
    }

    // ─── Single outbound: always selected ────────────────────────────────────

    @Test
    fun `single outbound is always selected`() = runTest {
        val outbound = FakeOutbound(Provider.OPENAI, "gpt-4o")
        val interceptor = RoutingInterceptor(listOf(outbound))
        val capturing = CapturingChain()

        interceptor.handle(fakeContext(), capturing)

        val ctx = capturing.lastContext
        assertNotNull(ctx)
        assertEquals("openai", ctx.request.provider.value)
        assertEquals("gpt-4o", ctx.request.model)
    }

    @Test
    fun `single outbound context request is rewritten with outbound provider and model`() = runTest {
        val outbound = FakeOutbound(Provider.ANTHROPIC, "claude-3-sonnet")
        val interceptor = RoutingInterceptor(listOf(outbound))
        val capturing = CapturingChain()

        // Original context has openai/gpt-4o but outbound is anthropic/claude
        interceptor.handle(fakeContext(Provider.OPENAI, "gpt-4o"), capturing)

        val ctx = capturing.lastContext!!
        assertEquals("anthropic", ctx.request.provider.value)
        assertEquals("claude-3-sonnet", ctx.request.model)
    }

    // ─── Multiple outbounds: one is always selected ───────────────────────────

    @Test
    fun `multiple outbounds exactly one is selected per call`() = runTest {
        val outbounds = listOf(
            FakeOutbound(Provider.OPENAI, "gpt-4o"),
            FakeOutbound(Provider.ANTHROPIC, "claude-3"),
            FakeOutbound(Provider.GEMINI, "gemini-pro")
        )
        val interceptor = RoutingInterceptor(outbounds)

        // Run 20 times and verify that the selected outbound is always one of the known providers
        val validProviders = setOf("openai", "anthropic", "gemini")
        repeat(20) {
            val capturing = CapturingChain()
            interceptor.handle(fakeContext(), capturing)
            val selected = capturing.lastContext?.request?.provider?.value
            assertNotNull(selected)
            assertTrue(selected in validProviders,
                "Selected provider '$selected' is not among the registered outbounds")
        }
    }

    // ─── Context attributes and mode are preserved ────────────────────────────

    @Test
    fun `routing preserves existing context attributes`() = runTest {
        val outbound = FakeOutbound(Provider.OPENAI, "gpt-4o")
        val interceptor = RoutingInterceptor(listOf(outbound))
        val capturing = CapturingChain()

        val ctx = fakeContext()
        ctx.attributes.put("tenant.id", "acme")

        interceptor.handle(ctx, capturing)

        val forwarded = capturing.lastContext!!
        assertEquals("acme", forwarded.attributes.get<String>("tenant.id"))
    }

    @Test
    fun `routing preserves request mode`() = runTest {
        val outbound = FakeOutbound(Provider.OPENAI, "gpt-4o")
        val interceptor = RoutingInterceptor(listOf(outbound))
        val capturing = CapturingChain()

        interceptor.handle(
            fakeContext(mode = RequestMode.STREAM),
            capturing
        )

        assertEquals(
            org.omniai.sdk.application.pipeline.RequestMode.STREAM,
            capturing.lastContext?.mode
        )
    }

    // ─── Chain result is passed through ──────────────────────────────────────

    @Test
    fun `result from chain is returned as-is`() = runTest {
        val outbound = FakeOutbound(Provider.OPENAI, "gpt-4o")
        val interceptor = RoutingInterceptor(listOf(outbound))
        val response = fakeResponse(id = "routing-resp")

        val result = interceptor.handle(
            fakeContext(),
            StaticChain(PipelineResult.Unary(response))
        )

        assertIs<PipelineResult.Unary>(result)
        assertEquals("routing-resp", result.response.id)
    }
}
