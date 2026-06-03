# OmniAI Gateway: Project Context Document

## 1. Overview and Objective
The OmniAI Gateway project solves the problem of technological fragmentation in interacting with different Large Language Models (LLMs). Currently, applications must integrate specific and disparate access interfaces to use models like OpenAI, Anthropic, or Gemini. The objective of this project is to build an **OmniAI SDK**, a unified layer that abstracts this communication, minimizing technological coupling and integration effort. 

The Gateway centralizes AI request management, providing robust resilience, fallback capabilities, streaming execution, and enterprise-grade security while standardizing request payloads using a singular overarching schema.

## 2. Technology Stack
The platform spans across standard tools to enable multi-platform integration and resilience logic:
*   **Core Languages:** Kotlin (Primary), Python (for isolated CLI runner scripts), Markdown/LaTeX (docs)
*   **Kotlin Multiplatform (KMP):** The core SDK (`OmniAi-SDK`) is engineered with KMP, ensuring domain logic and pipeline interceptors can be consumed safely inJVM and Node.js environments.
*   **Backend & Networking:** Ktor (Ktor Server, Ktor HTTP Client) for mapping the HTTP routes and network connectivity; Netty (Server engine).
*   **Security & Auth:** OAuth 2.0 / OIDC integrations utilizing Logto as the identity provider (IdP). JSON Web Tokens (JWT) are validated across interceptor chains.
*   **Telemetry & Observability:** OpenTelemetry (OTLP), Prometheus, and Grafana (for gathering and portraying token metrics and request errors).
*   **Build System:** Gradle (Composite builds with `includeBuild`).

## 3. System Architecture
The system aligns directly with **Hexagonal Architecture (Ports and Adapters)** to segregate domain logic from third-party structures.
*   **Core Domain:** Contains platform-agnostic models (`CommonRequest`, `GatewayContext`, `PipelineResult`) and pipeline execution logic.
*   **Inbound Adapters:** Translate provider-specific formats (e.g., standard OpenAI `chat/completions` JSON inputs or Gemini formats) into the unified domain model (`CommonRequest`). These inbound interfaces allow any client familiar with one API format to plug directly into the gateway.
*   **Outbound Adapters:** Form the core interaction layer that transforms `CommonRequest` objects back into the real provider's exact HTTP formats (leveraging the explicit `OutboundPort`).
*   **Interceptor Pipeline:** Situated between inbounds and outbounds. Using a Chain-of-Responsibility pattern, interceptors act upon a `GatewayContext` modifying logic like authentication, telemetry extraction, circuit breaking, and fallbacks cleanly.

## 4. Integrations and Models
The Gateway integrates natively with the three fundamental provider ecosystems: **OpenAI**, **Anthropic**, and **Gemini**.
*   **Bidirectional Parsing:** A single request containing standardized messages can be parsed by the OpenAi Inbound adapter, and routed by the dispatcher to a Gemini Outbound adapter—perfectly mapping tool usages and parameters.
*   **Streaming & Unary Execution:** Full support for `flowOf` data structures inside Ktor allows real-time async chunk generation (`generateStream`) alongside traditional unary calls.
*   **Model Context Protocol (MCP) Broker:** Includes dedicated infrastructure to map resources, prompts, and tool calling definitions. Currently abstracts the MCP server initialization and schema translation for bridging external environments over Stdio / SSE / WebSockets.

## 5. Resilience and Security Mechanisms
Robust interceptors secure the core flow before delegating it to AI services:
*   **Circuit Breaker (`CircuitBreakerInterceptor`):** Tracks successes and failures of outgoing connections. If an outbound endpoint surpasses a `failureThreshold`, the circuit transitions to `OPEN`, immediately avoiding upstream penalties and responding with an `ApiDownError`.
*   **Fallback (`FallbackInterceptor`):** Uses an explicit strategy where sequences of alternative valid models are tracked. If a `primary` outbound model produces an error, it mutates the request internally and retries against alternate available endpoints continuously until yielding a valid result.
*   **Security / Authentication:** `AuthenticationInterceptor` validates bearer tokens resolving them into unverified/verified OIDC/JWT domains. These identities map client IDs and emails via `AuthValidationResult.Jwt`. Subsequent `PolicyEnforcerInterceptor` applies dynamic logic checking permissions.

## 6. Directory and Module Structure
```text
project-management/
 ├── OmniAi-SDK/ (KMP Core)
 │   ├── core/                  # Core Hexagonal domain, Pipeline Interfaces, Ports
 │   ├── dispatcher/            # Routing mapping inbound queries to target outbounds
 │   ├── inbound/               # Provider-specific listener adapters (openai, gemini, anthropic)
 │   ├── outbound/              # Provider-specific execution callers
 │   ├── interceptors/          # Fallbacks, Circuit Breaking, Auth, Telemetry
 │   ├── mcp-broker/            # Tooling and Resource proxy utilizing Model Context Protocol
 │   ├── gateway-client/        # DSL structure enabling quick programatic wiring
 │   └── gateway-ktor-server/   # Ktor extension bridging routes to inbound connectors
 ├── OmniAiGateway/             # Host application bootstrapping Ktor, parsing configs
 ├── docs/                      # Thesis reports / LaTeX code
 └── run_claude.py              # CLI demo fetching Logto OIDC token & executing Claude Code
```

## 7. Key Code Snippets

### 1. The Outbound Adapter Contract (`OutboundPort.kt`)
Represents the common schema required by external models.
```kotlin
package org.omniai.sdk.core.ports

interface OutboundPort {
    val provider: Provider
    val model: Model
    val key: String

    suspend fun generate(request: CommonRequest): Either<DomainError, CommonResponse>
    suspend fun generateStream(request: CommonRequest): Either<DomainError, Flow<CommonResponseEvent>>
}
```

### 2. Request Handling Pipeline (`GatewayPipelineChain.kt`)
Executes interceptors generically before calling the routing dispatcher.
```kotlin
internal class GatewayPipelineChain(
    private val interceptors: List<Interceptor>,
    private val dispatcher: DispatcherPort,
    private val index: Int = 0
) : InterceptorChain {
    override suspend fun proceed(context: GatewayContext): PipelineResult {
        if (index >= interceptors.size) {
            // Reached the end of the chain -> Execute target call
            return when (context.res) {
                is PipelineResult.NoResult -> when (context.mode) {
                    RequestMode.UNARY -> when (val result = dispatcher.generate(context.request, context.attributes)) {
                        is Either.Right -> PipelineResult.Unary(result.value)
                        is Either.Left -> PipelineResult.Error(result.value)
                    }
                    // omitted stream block for brevity
                }
                // omitted generic returning blocks 
            }
        }
        val nextChain = GatewayPipelineChain(interceptors, dispatcher, index + 1)
        return interceptors[index].handle(context, nextChain)
    }
}
```

### 3. Circuit Breaker Mechanism (`CircuitBreakerInterceptor.kt`)
Monitors the state of Outbounds dynamically.
```kotlin
val outboundId = targetOutbound.key
val currentState = store.getState(outboundId)

if (currentState == CircuitState.OPEN) {
    val error = ApiDownError("Circuit breaker is OPEN for outbound: $outboundId")
    return PipelineResult.Error(error)
}

val result = chain.proceed(context)

when (result) {
    is PipelineResult.Error -> {
        store.recordFailure(outboundId)
        val failures = store.getFailures(outboundId)
        if (failures >= config.failureThreshold && currentState != CircuitState.OPEN) {
            store.transitionState(outboundId, CircuitState.OPEN)
        }
    }
}
```

### 4. System Bootstrap & DSL Setup (`Main.kt`)
Building the components using the SDK's DSL.
```kotlin
val gateway = omniAiGateway {
    execution {
        useNativePipeline {
            outbounds {
                config.providers.forEach { providerConfig ->
                    when (providerConfig.provider) {
                        ProviderKind.OPENAI -> openAI(httpClient) {
                            baseUrl(providerConfig.baseUrl)
                            apiKey(providerConfig.apiKey) {
                                models(*providerConfig.models.toTypedArray())
                            }
                        }
                    }
                }
            }
            interceptors {
                metrics {
                    metricsPort = telemetryRuntime.metricsPort
                    // attribute extraction
                }
            }
        }
    }
}
```
