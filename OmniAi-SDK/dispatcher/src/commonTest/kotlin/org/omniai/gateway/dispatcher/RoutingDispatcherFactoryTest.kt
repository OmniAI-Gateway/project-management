package org.omniai.gateway.dispatcher

import org.omniai.sdk.common.success
import org.omniai.sdk.common.Either
import org.omniai.sdk.common.onFailure
import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent
import org.omniai.sdk.domain.errors.UnknownDomainError
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoutingDispatcherFactoryTest {

    private val openAiProvider = Provider("openai")
    private val gpt4Model = Model("gpt-4")

    private val anthropicProvider = Provider("anthropic")
    private val claudeModel = Model("claude-3")

    @Test
    fun `should route unary request to the matching outbound port`() = runTest {
        // Arrange
        val expectedResponse = success(CommonResponse(provider = openAiProvider, id = "id1", model = gpt4Model.model, choices = emptyList()))
        val targetOutbound = FakeOutboundAdapter(provider = openAiProvider, model = gpt4Model).apply {
            stubbedUnaryResponse = expectedResponse
        }
        val otherOutbound = FakeOutboundAdapter(provider = anthropicProvider, model = claudeModel)

        val dispatcher = routingDispatcherFactory(listOf(otherOutbound, targetOutbound))
        val request = CommonRequest(provider = openAiProvider, model = gpt4Model.model, messages = emptyList(), providerOptions = TypedMap())

        // Act
        // Note: You need to call the appropriate method on DispatcherPort.
        // Assuming your dispatcherAdapter exposes generate/unary somehow,
        // or testing the underlying logic.
        val result = dispatcher.generate(request, TypedMap())

        // Assert
        assertEquals(expectedResponse, result)
        assertEquals(request.provider, targetOutbound.receivedRequest?.provider)
    }

    @Test
    fun `should return UnknownDomainError when no outbound port matches unary request`() = runTest {
        // Arrange
        val availableOutbound = FakeOutboundAdapter(provider = anthropicProvider, model = claudeModel)
        val dispatcher = routingDispatcherFactory(listOf(availableOutbound))

        // Requesting OpenAI, but only Anthropic is available
        val request = CommonRequest(provider = openAiProvider, model = gpt4Model.model, messages = emptyList(), providerOptions = TypedMap())

        // Act
        val result = dispatcher.generate(request, TypedMap())

        // Assert
        assertTrue(result is Either.Left)
        result.onFailure { error ->
            assertTrue(error is UnknownDomainError)
            assertEquals("No outbound available for provider=openai model=gpt-4", error.message)
        }
    }

    @Test
    fun `should merge attributes into providerOptions before calling outbound port`() = runTest {
        // Arrange
        val targetOutbound = FakeOutboundAdapter(provider = openAiProvider, model = gpt4Model).apply {
            stubbedUnaryResponse = success(CommonResponse(provider = openAiProvider, id = "id1", model = gpt4Model.model, choices = emptyList()))
        }
        val dispatcher = routingDispatcherFactory(listOf(targetOutbound))

        val initialOptions = TypedMap().apply { put("temperature", 0.5) }
        val request = CommonRequest(provider = openAiProvider, model = gpt4Model.model, messages = emptyList(), providerOptions = initialOptions)

        val runtimeAttributes = TypedMap().apply { put("max_tokens", 100) }

        // Act
        dispatcher.generate(request, runtimeAttributes)

        // Assert
        val forwardedRequest = targetOutbound.receivedRequest
        assertTrue(forwardedRequest != null, "Request should have been forwarded")

        // Verify both original options and new attributes are present
        assertEquals(0.5, forwardedRequest.providerOptions.get<Double>("temperature"))
        assertEquals(100, forwardedRequest.providerOptions.get<Int>("max_tokens"))
    }

    @Test
    fun `should route stream request to the matching outbound port`() = runTest {
        // Arrange
        val expectedStream = success(emptyFlow<CommonResponseEvent>())
        val targetOutbound = FakeOutboundAdapter(provider = anthropicProvider, model = claudeModel).apply {
            stubbedStreamResponse = expectedStream
        }

        val dispatcher = routingDispatcherFactory(listOf(targetOutbound))
        val request = CommonRequest(provider = anthropicProvider, model = claudeModel.model, messages = emptyList(), providerOptions = TypedMap())

        // Act
        val result = dispatcher.generateStream(request, TypedMap())

        // Assert
        assertEquals(expectedStream, result)
        assertEquals(request.provider, targetOutbound.receivedRequest?.provider)
    }
}