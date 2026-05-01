package org.omniai.gateway.app


import org.omniai.sdk.interceptors.metrics.TelemetryMeter
import org.omniai.sdk.interceptors.metrics.TelemetryTracer

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
    val baseUrlEnv: String
) {
    OPENAI(
        configKey = "openai",
        apiKeyEnv = "OPENAI_API_KEY",
        modelEnv = "OPENAI_MODEL",
        modelsEnv = "OPENAI_MODELS",
        baseUrlEnv = "OPENAI_BASE_URL"
    ),
    GEMINI(
        configKey = "gemini",
        apiKeyEnv = "GEMINI_API_KEY",
        modelEnv = "GEMINI_MODEL",
        modelsEnv = "GEMINI_MODELS",
        baseUrlEnv = "GEMINI_BASE_URL"
    ),
    ANTHROPIC(
        configKey = "anthropic",
        apiKeyEnv = "ANTHROPIC_API_KEY",
        modelEnv = "ANTHROPIC_MODEL",
        modelsEnv = "ANTHROPIC_MODELS",
        baseUrlEnv = "ANTHROPIC_BASE_URL"
    )
}

