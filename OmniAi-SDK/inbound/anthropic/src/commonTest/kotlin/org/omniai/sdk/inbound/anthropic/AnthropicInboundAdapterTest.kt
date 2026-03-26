package org.omniai.sdk.inbound.anthropic

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.omniai.sdk.contracts.anthropic.input.AnthropicInputContentBlock
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessageInput
import org.omniai.sdk.contracts.anthropic.input.AnthropicMessagesRequest
import org.omniai.sdk.contracts.anthropic.input.ListContentBlock
import org.omniai.sdk.contracts.anthropic.output.AnthropicStreamEvent
import org.omniai.sdk.core.ports.InferenceServicePort
import org.omniai.sdk.domain.common.CommonRole
import org.omniai.sdk.domain.common.Model
import org.omniai.sdk.domain.common.Provider
import org.omniai.sdk.domain.common.content.TextPart
import org.omniai.sdk.domain.requests.CommonRequest
import org.omniai.sdk.domain.responses.CommonChoice
import org.omniai.sdk.domain.responses.CommonResponse
import org.omniai.sdk.domain.responses.CommonResponseEvent
import org.omniai.sdk.domain.responses.CommonResponseMessage
import org.omniai.sdk.domain.responses.ResponseStarted

class AnthropicInboundAdapterTest {

//    @Test
//    fun `generate translates anthropic request and maps service response`() = runBlocking {
//        var receivedRequest: CommonRequest? = null
//
//        val service = object : InferenceServicePort {
//            override suspend fun generate(request: CommonRequest): CommonResponse {
//                receivedRequest = request
//                return CommonResponse(
//                    provider = Provider.ANTHROPIC,
//                    id = "msg-1",
//                    model = "claude-3-5-sonnet",
//                    choices = listOf(
//                        CommonChoice(
//                            index = 0,
//                            message = CommonResponseMessage(
//                                role = CommonRole.ASSISTANT,
//                                content = listOf(TextPart("ok"))
//                            )
//                        )
//                    )
//                )
//            }
//
//            override fun generateStream(request: CommonRequest): Flow<CommonResponseEvent> = flowOf()
//        }
//
//        val adapter = AnthropicInboundAdapter(service)
//        val request = anthropicRequest()
//
//        val response = adapter.generate(request)
//
//        assertEquals(Provider.ANTHROPIC, receivedRequest?.provider)
//        assertEquals("claude-3-5-sonnet", receivedRequest?.model)
//        assertEquals("msg-1", response.id)
//        assertEquals("assistant", response.role)
//        assertEquals("ok", (response.content.first() as org.omniai.sdk.contracts.anthropic.output.AnthropicOutputContent.Text).text)
//    }
//
//    @Test
//    fun `generateStream delegates to service and maps domain events`() = runBlocking {
//        val service = object : InferenceServicePort {
//            override suspend fun generate(request: CommonRequest): CommonResponse {
//                throw UnsupportedOperationException("not used in this test")
//            }
//
//            override fun generateStream(request: CommonRequest): Flow<CommonResponseEvent> = flowOf(
//                ResponseStarted(
//                    provider = Provider.ANTHROPIC,
//                    id = "msg-stream",
//                    model = Model("claude-3-5-sonnet"),
//                    sequence = 1
//                )
//            )
//        }
//
//        val adapter = AnthropicInboundAdapter(service)
//
//        val firstEvent = adapter.generateStream(anthropicRequest()).first()
//
//        assertTrue(firstEvent is AnthropicStreamEvent.MessageStart)
//        assertEquals("msg-stream", firstEvent.message.id)
//    }
//
//    private fun anthropicRequest(): AnthropicMessagesRequest = AnthropicMessagesRequest(
//        model = "claude-3-5-sonnet",
//        maxTokens = 128,
//        messages = listOf(
//            AnthropicMessageInput(
//                role = "user",
//                content = ListContentBlock(
//                    blocks = listOf(
//                        AnthropicInputContentBlock.Text("hello")
//                    )
//                )
//            )
//        )
//    )
}

