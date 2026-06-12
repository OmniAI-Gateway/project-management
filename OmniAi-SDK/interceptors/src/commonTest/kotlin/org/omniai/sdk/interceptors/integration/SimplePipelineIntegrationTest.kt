package org.omniai.sdk.interceptors.integration

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.omniai.sdk.application.pipeline.GatewayPipeline
import org.omniai.sdk.application.pipeline.GatewayPipelineBuilder
import org.omniai.sdk.common.Either
import org.omniai.sdk.common.failure
import org.omniai.sdk.common.key
import org.omniai.sdk.common.success
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.errors.UnknownDomainError
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent
import org.omniai.sdk.interceptors.helper.FakeOutbound
import org.omniai.sdk.interceptors.circuitBreaker.CircuitBreakerConfig
import org.omniai.sdk.interceptors.circuitBreaker.CircuitBreakerInterceptor
import org.omniai.sdk.interceptors.circuitBreaker.CircuitState
import org.omniai.sdk.interceptors.circuitBreaker.InMemoryCircuitBreakerStore
import org.omniai.sdk.interceptors.helper.fakeContext
import org.omniai.sdk.interceptors.helper.fakeError
import org.omniai.sdk.interceptors.helper.fakeResponse
import org.omniai.sdk.interceptors.fallback.FallbackInterceptor
import org.omniai.sdk.ports.inbound.DispatcherPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SimplePipelineIntegrationTest {

    private val deniedKey = key<Set<String>>("deniedOutbounds")

    @Test
    fun `open circuit on primary falls back to secondary`() = runTest {
        val primary = FakeOutbound(Provider.OPENAI, "gpt-4o")
        val secondary = FakeOutbound(Provider.ANTHROPIC, "claude-3")
        val store = InMemoryCircuitBreakerStore().apply {
            transitionState(primary.key, CircuitState.OPEN)
        }

        val dispatcher = RecordingDispatcher(
            unaryByOutbound = mapOf(
                primary.key to failure(fakeError("primary down")),
                secondary.key to success(fakeResponse(provider = Provider.ANTHROPIC, model = "claude-3"))
            )
        )

        val pipeline = buildPipeline(
            store = store,
            outbounds = listOf(primary, secondary),
            dispatcher = dispatcher,
            failureThreshold = 1
        )

        val result = pipeline.executeUnary(fakeContext(provider = Provider.OPENAI, model = "gpt-4o"))

        val ok = assertIs<Either.Right<CommonResponse>>(result)
        assertEquals("anthropic", ok.value.provider.value)
        assertEquals(listOf("anthropic:claude-3"), dispatcher.unaryCalls)
    }

    @Test
    fun `primary failure opens circuit and next request skips primary dispatcher call`() = runTest {
        val primary = FakeOutbound(Provider.OPENAI, "gpt-4o")
        val secondary = FakeOutbound(Provider.ANTHROPIC, "claude-3")
        val store = InMemoryCircuitBreakerStore()

        val dispatcher = RecordingDispatcher(
            unaryByOutbound = mapOf(
                primary.key to failure(fakeError("primary down")),
                secondary.key to success(fakeResponse(provider = Provider.ANTHROPIC, model = "claude-3"))
            )
        )

        val pipeline = buildPipeline(
            store = store,
            outbounds = listOf(primary, secondary),
            dispatcher = dispatcher,
            failureThreshold = 1
        )

        val first = pipeline.executeUnary(fakeContext(provider = Provider.OPENAI, model = "gpt-4o"))
        val second = pipeline.executeUnary(fakeContext(provider = Provider.OPENAI, model = "gpt-4o"))

        assertIs<Either.Right<CommonResponse>>(first)
        assertIs<Either.Right<CommonResponse>>(second)
        assertEquals(CircuitState.OPEN, store.getState(primary.key))
        assertEquals(1, dispatcher.unaryCalls.count { it == primary.key })
        assertEquals(2, dispatcher.unaryCalls.count { it == secondary.key })
    }

    private fun buildPipeline(
        store: InMemoryCircuitBreakerStore,
        outbounds: List<FakeOutbound>,
        dispatcher: DispatcherPort,
        failureThreshold: Int
    ): GatewayPipeline {
        return GatewayPipelineBuilder().apply {
            // Fallback wraps circuit-breaker and retries with another outbound when needed.
            install(
                FallbackInterceptor(
                    outbounds = outbounds,
                    deniedOutboundsKey = deniedKey
                )
            )
            install(
                CircuitBreakerInterceptor(
                    store = store,
                    config = CircuitBreakerConfig(failureThreshold = failureThreshold),
                    deniedOutboundsKey = deniedKey,
                    outbounds = outbounds
                )
            )
            installDispatcher(dispatcher)
        }.build()
    }
}

private class RecordingDispatcher(
    private val unaryByOutbound: Map<String, Either<DomainError, CommonResponse>>
) : DispatcherPort {
    val unaryCalls = mutableListOf<String>()

    override suspend fun generate(
        request: CommonRequest,
        attributes: org.omniai.sdk.common.TypedMap
    ): Either<DomainError, CommonResponse> {
        val key = "${request.provider.value}:${request.model}"
        unaryCalls += key
        return unaryByOutbound[key] ?: failure(UnknownDomainError("No unary stub for outbound: $key"))
    }

    override suspend fun generateStream(
        request: CommonRequest,
        attributes: org.omniai.sdk.common.TypedMap
    ): Either<DomainError, Flow<CommonResponseEvent>> {
        return success(emptyFlow())
    }
}

