package org.omniai.sdk.inbound.anthropic

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.omniai.sdk.contracts.anthropic.input.AnthropicInputContentBlock
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessageInput
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.input.AnthropicRole
import org.omniai.sdk.contracts.anthropic.input.ListContentBlock
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent
import org.omniai.sdk.common.Either
import org.omniai.sdk.common.Success
import org.omniai.sdk.common.TypedMap
import org.omniai.sdk.common.success
import org.omniai.sdk.ports.inbound.DispatcherPort
import org.omniai.sdk.domain.common.CommonRole
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.common.content.TextPart
import org.omniai.sdk.domain.errors.DomainError
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonChoice
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent
import org.omniai.sdk.domain.responses.CommonResponseMessage
import org.omniai.sdk.domain.responses.ResponseStarted

class AnthropicInboundAdapterTest {

	@Test
	fun `generate translates anthropic request and maps service response`() = runTest {
		var receivedRequest: CommonRequest? = null

		val service = object : DispatcherPort {
			override suspend fun generate(
                request: CommonRequest,
                attributes: TypedMap
            ): Either<DomainError, CommonResponse> {
				receivedRequest = request
				return success(
					CommonResponse(
						provider = Provider.ANTHROPIC,
						id = "msg-1",
						model = "claude-3-5-sonnet",
						choices = listOf(
							CommonChoice(
								index = 0,
								message = CommonResponseMessage(
									role = CommonRole.ASSISTANT,
									content = listOf(TextPart("ok"))
								)
							)
						)
					)
				)
			}

			override suspend fun generateStream(
                request: CommonRequest,
                attributes: TypedMap
            ): Either<DomainError, Flow<CommonResponseEvent>> = success(flowOf())
		}

		val adapter = AnthropicInboundAdapter(service)
		val metadata = TypedMap().also { it.put("traceId", "trace-123") }

		val result = adapter.generate(anthropicRequest(), metadata)
		assertTrue(result is Success)

		val response = result.value
		assertEquals(Provider.ANTHROPIC, receivedRequest?.provider)
		assertEquals("claude-3-5-sonnet", receivedRequest?.model)
		assertEquals("trace-123", receivedRequest?.providerOptions?.get<String>("traceId"))
		assertEquals("msg-1", response.id)
		assertEquals("assistant", response.role)
		assertEquals(
			"ok",
			(response.content.first() as org.omniai.sdk.contracts.anthropic.output.AnthropicOutputContent.Text).text
		)
	}

	@Test
	fun `generateStream delegates to service and maps domain events`() = runTest {
		val service = object : DispatcherPort {
			override suspend fun generate(
                request: CommonRequest,
                attributes: TypedMap
            ): Either<DomainError, CommonResponse> {
				throw UnsupportedOperationException("not used in this test")
			}

			override suspend fun generateStream(
                request: CommonRequest,
                attributes: TypedMap
            ): Either<DomainError, Flow<CommonResponseEvent>> = success(
				flowOf(
					ResponseStarted(
						provider = Provider.ANTHROPIC,
						id = "msg-stream",
						model = Model("claude-3-5-sonnet"),
						sequence = 1
					)
				)
			)
		}

		val adapter = AnthropicInboundAdapter(service)
		val result = adapter.generateStream(anthropicRequest(), TypedMap())
		assertTrue(result is Success)

		val firstEvent = result.value.first()
		assertTrue(firstEvent is AnthropicStreamEvent.MessageStart)
		assertEquals("msg-stream", firstEvent.message.id)
	}

	private fun anthropicRequest(): AnthropicMessagesRequest = AnthropicMessagesRequest(
		model = "claude-3-5-sonnet",
		maxTokens = 128,
		messages = listOf(
			AnthropicMessageInput(
				role = AnthropicRole.USER,
				content = ListContentBlock(
					blocks = listOf(
						AnthropicInputContentBlock.Text("hello")
					)
				)
			)
		)
	)
}


