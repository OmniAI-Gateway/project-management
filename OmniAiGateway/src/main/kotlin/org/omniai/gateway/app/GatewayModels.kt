package org.omniai.gateway.app

import org.omniai.gateway.metrics.TelemetryMeter
import org.omniai.gateway.metrics.TelemetryTracer
import org.omniai.sdk.adapters.anthropic.AnthropicOutboundAdapter
import org.omniai.sdk.adapters.gemini.GeminiOutboundAdapter
import org.omniai.sdk.adapters.openai.OpenAiOutboundAdapter
import org.omniai.sdk.core.ports.OutboundPort
import kotlin.reflect.KClass

data class GatewayConfig(
    val port: Int,
    val providers: List<ProviderConfig>,
    val telemetryEnabled: Boolean,
    val otelEnabled: Boolean,
    val otelCollectorEndpoint: String?
)

data class TelemetryRuntime(
    val meter: TelemetryMeter,
    val tracer: TelemetryTracer? = null
)

data class ProviderConfig(
    val provider: ProviderKind,
    val models: List<String>,
    val apiKey: String,
    val baseUrl: String
)

enum class ProviderKind(
    val configKey: String,
    val apiKeyEnv: String,
    val modelEnv: String,
    val modelsEnv: String,
    val baseUrlEnv: String,
    val outboundFactory: () -> KClass<out OutboundPort>
) {
    OPENAI(
        configKey = "openai",
        apiKeyEnv = "OPENAI_API_KEY",
        modelEnv = "OPENAI_MODEL",
        modelsEnv = "OPENAI_MODELS",
        baseUrlEnv = "OPENAI_BASE_URL",
        outboundFactory = { OpenAiOutboundAdapter::class }
    ),
    GEMINI(
        configKey = "gemini",
        apiKeyEnv = "GEMINI_API_KEY",
        modelEnv = "GEMINI_MODEL",
        modelsEnv = "GEMINI_MODELS",
        baseUrlEnv = "GEMINI_BASE_URL",
        outboundFactory = { GeminiOutboundAdapter::class }
    ),
    ANTHROPIC(
        configKey = "anthropic",
        apiKeyEnv = "ANTHROPIC_API_KEY",
        modelEnv = "ANTHROPIC_MODEL",
        modelsEnv = "ANTHROPIC_MODELS",
        baseUrlEnv = "ANTHROPIC_BASE_URL",
        outboundFactory = { AnthropicOutboundAdapter::class }
    )
}

