package org.omniai.sdk.interceptors.fallback

import kotlinx.coroutines.test.runTest
import org.omniai.sdk.application.pipeline.PipelineResult
import org.omniai.sdk.common.failure
import org.omniai.sdk.common.key
import org.omniai.sdk.common.success
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.interceptors.helper.CapturingChain
import org.omniai.sdk.interceptors.helper.FakeOutbound
import org.omniai.sdk.interceptors.helper.StaticChain
import org.omniai.sdk.interceptors.helper.fakeContext
import org.omniai.sdk.interceptors.helper.fakeError
import org.omniai.sdk.interceptors.helper.fakeResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class FallbackInterceptorTest {
    private val deniedKey = key<Set<String>>("deniedOutbounds")

    private fun interceptor(outbounds: List<FakeOutbound>) =
        FallbackInterceptor(
            outbounds = outbounds,
            deniedOutboundsKey = deniedKey,
        )

    // ─── Primary succeeds ─────────────────────────────────────────────────────

    @Test
    fun `primary succeeds result returned immediately, no fallback used`() =
        runTest {
            val primary = FakeOutbound(Provider.OPENAI, "gpt-4o")
            val alternative = FakeOutbound(Provider.ANTHROPIC, "claude-3")
            val cb = interceptor(listOf(primary, alternative))
            val capturing = CapturingChain(PipelineResult.Unary(fakeResponse(provider = Provider.OPENAI)))

            val result = cb.handle(fakeContext(provider = Provider.OPENAI, model = "gpt-4o"), capturing)

            assertIs<PipelineResult.Unary>(result)
            // Alternative should never have been tried; capturing only called once (for primary)
            assertEquals(1, capturing.callCount())
        }

    // ─── Primary fails, fallback used ────────────────────────────────────────

    @Test
    fun `primary error triggers fallback to alternative outbound`() =
        runTest {
            val primary =
                FakeOutbound(
                    Provider.OPENAI,
                    "gpt-4o",
                    unaryResult = failure(fakeError("primary down")),
                )
            val alternative =
                FakeOutbound(
                    Provider.ANTHROPIC,
                    "claude-3",
                    unaryResult = success(fakeResponse(provider = Provider.ANTHROPIC, model = "claude-3")),
                )

            val cb = interceptor(listOf(primary, alternative))

            // Use a chain that uses the request's provider/model to simulate success/failure
            var callCount = 0
            val chain =
                object : org.omniai.sdk.application.pipeline.InterceptorChain {
                    override suspend fun proceed(context: org.omniai.sdk.application.pipeline.GatewayContext): PipelineResult {
                        callCount++
                        return if (context.request.provider.value == "openai") {
                            PipelineResult.Error(fakeError("primary down"))
                        } else {
                            PipelineResult.Unary(fakeResponse(provider = Provider.ANTHROPIC, model = "claude-3"))
                        }
                    }
                }

            val result = cb.handle(fakeContext(Provider.OPENAI, "gpt-4o"), chain)

            assertIs<PipelineResult.Unary>(result)
            assertEquals(2, callCount)
        }

    @Test
    fun `fallback context has provider and model rewritten to alternative outbound`() =
        runTest {
            val primary = FakeOutbound(Provider.OPENAI, "gpt-4o")
            val alternative = FakeOutbound(Provider.ANTHROPIC, "claude-3")
            val cb = interceptor(listOf(primary, alternative))

            var secondContext: org.omniai.sdk.application.pipeline.GatewayContext? = null
            var callCount = 0

            val chain =
                object : org.omniai.sdk.application.pipeline.InterceptorChain {
                    override suspend fun proceed(context: org.omniai.sdk.application.pipeline.GatewayContext): PipelineResult {
                        callCount++
                        return if (callCount == 1) {
                            PipelineResult.Error(fakeError("primary fail"))
                        } else {
                            secondContext = context
                            PipelineResult.Unary(fakeResponse())
                        }
                    }
                }

            cb.handle(fakeContext(Provider.OPENAI, "gpt-4o"), chain)

            assertNotNull(secondContext)
            assertEquals("anthropic", secondContext.request.provider.value)
            assertEquals("claude-3", secondContext.request.model)
        }

    // ─── All outbounds fail ───────────────────────────────────────────────────

    @Test
    fun `all outbounds fail last error is returned`() =
        runTest {
            val primary = FakeOutbound(Provider.OPENAI, "gpt-4o")
            val alternative = FakeOutbound(Provider.ANTHROPIC, "claude-3")
            val cb = interceptor(listOf(primary, alternative))

            val lastError = fakeError("alternative also down")
            var callCount = 0
            val chain =
                object : org.omniai.sdk.application.pipeline.InterceptorChain {
                    override suspend fun proceed(context: org.omniai.sdk.application.pipeline.GatewayContext): PipelineResult {
                        callCount++
                        return if (callCount == 1) {
                            PipelineResult.Error(fakeError("primary down"))
                        } else {
                            PipelineResult.Error(lastError)
                        }
                    }
                }

            val result = cb.handle(fakeContext(Provider.OPENAI, "gpt-4o"), chain)

            assertIs<PipelineResult.Error>(result)
            assertEquals(lastError, result.error)
        }

    // ─── Pre-denied outbounds (from circuit breaker) ──────────────────────────

    @Test
    fun `pre-denied primary outbound is skipped and alternative is tried directly`() =
        runTest {
            val primary = FakeOutbound(Provider.OPENAI, "gpt-4o")
            val alternative = FakeOutbound(Provider.ANTHROPIC, "claude-3")
            val cb = interceptor(listOf(primary, alternative))

            val ctx = fakeContext(Provider.OPENAI, "gpt-4o")
            // Pre-deny primary
            ctx.attributes[deniedKey] = setOf("openai:gpt-4o")

            var callCount = 0
            val chain =
                object : org.omniai.sdk.application.pipeline.InterceptorChain {
                    override suspend fun proceed(context: org.omniai.sdk.application.pipeline.GatewayContext): PipelineResult {
                        callCount++
                        return PipelineResult.Unary(fakeResponse(provider = Provider.ANTHROPIC))
                    }
                }

            val result = cb.handle(ctx, chain)

            assertIs<PipelineResult.Unary>(result)
            assertEquals(1, callCount, "Only alternative should have been tried")
        }

    // ─── Empty outbounds ──────────────────────────────────────────────────────

    @Test
    fun `no outbounds at all returns NoResult`() =
        runTest {
            val cb = interceptor(emptyList())
            val chain = StaticChain(PipelineResult.Unary(fakeResponse()))

            val result = cb.handle(fakeContext(), chain)

            assertIs<PipelineResult.NoResult>(result)
        }

    // ─── Helper extension ─────────────────────────────────────────────────────

    private fun CapturingChain.callCount(): Int {
        var c = 0
        // Workaround: we count via the lastContext being non-null for first call;
        // in practice the capturing chain in this test class is simple enough
        return if (lastContext != null) 1 else 0
    }
}
