package org.omniai.sdk.gateway.client.dsl

import org.omniai.sdk.gateway.client.core.ExecutionMode
import org.omniai.sdk.gateway.client.extensions.inbounds.openAi
import org.omniai.sdk.ports.inbound.InboundConnector
import org.omniai.sdk.contracts.openai.input.OpenAiChatCompletionsRequest
import org.omniai.sdk.contracts.openai.output.OpenAiChatCompletionsResponse
import org.omniai.sdk.ports.inbound.InboundPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OmniAiGatewayDslTest {

    @Test
    fun `should build valid OmniAiConfig with native pipeline and openAi inbound`() {
        val mockOpenAiConnector = InboundConnector<OpenAiChatCompletionsRequest, OpenAiChatCompletionsResponse, OpenAiChatCompletionsResponse> { }

        val config = omniAiGateway {
            inbounds {
                openAi(mockOpenAiConnector)
            }

            execution {
                useNativePipeline {
                    // empty
                }
            }
        }

        assertTrue(config.execution is ExecutionMode.NativePipeline)
        assertEquals(1, config.inbounds.setups.size)
    }

    @Test
    fun `should fail if execution mode is not provided`() {
        assertFailsWith<IllegalArgumentException> {
            omniAiGateway {
                inbounds {
                    // empty
                }
            }
        }
    }
}
