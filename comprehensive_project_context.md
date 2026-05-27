# Comprehensive Project Context: OmniAi-SDK & Gateway
*(Exhaustive Audit Report)*

This document provides a deep, line-by-line architectural extraction and analysis of the `OmniAi-SDK` and `OmniAiGateway` codebase. It avoids summarization, pulling exact implementation details, pipeline structures, integration mappings, and the fundamental constraints built into the multiplatform Kotlin architecture.

---

## 1. Core Domain & Data Models

The system is built on a highly normalized internal domain that bridges the structural differences between OpenAI, Anthropic, and Gemini. Instead of mapping one-to-one with a specific provider, the `CommonRequest` and `CommonResponse` interfaces capture the maximal feature set of modern LLMs.

### **Request Normalization**
Found in `org.omniai.sdk.domain.requests.CommonRequest`:
The domain guarantees provider-agnostic handling with a structured request format:
```kotlin
data class CommonRequestMessage(
    val role: CommonRole, // User, System, Assistant, Tool
    val content: List<RequestContentPart> // Polymorphic content: TextPart, ToolCallPart, ToolResultPart
)

data class CommonRequest(
    val provider: Provider, // e.g. OPENAI, ANTHROPIC, GEMINI
    val model: String,
    val messages: List<CommonRequestMessage>,
    val systemPrompt: SystemPrompt? = null,
    val config: CommonGenerationConfig? = null,
    val tools: List<CommonTool> = emptyList(),
    val toolChoice: ToolChoice? = null,
    val jsonResponse: Boolean = false,
    val providerOptions: TypedMap = TypedMap()
)
```
**Handling Provider Differences:** Features that do not seamlessly align between providers (e.g., Anthropic's specific configuration requirements or OpenAI's granular seed/logit bias tweaks) are captured in `providerOptions`, an extension `TypedMap` allowing arbitrary keys at runtime (like `presencePenalty`, `frequencyPenalty`, `seed`, and `topLogProbs`). 

### **Response & Streaming Normalization**
Streaming is generalized via the `CommonResponseEvent` sealed hierarchy, defining explicit lifecycle stages:
- `ResponseStarted`
- `TextDeltaEvent`
- `ToolCallStartedEvent`
- `ToolCallArgumentsDeltaEvent`
- `ChoiceFinished`
- `ResponseCompleted` (and `ResponseErrored` with dynamic fallback routing context).

---

## 2. KMP & Multiplatform Architecture

This project is a strict **Kotlin Multiplatform (KMP)** software layer.
- **`core`, `contracts`, `interceptors`, `mcp-broker`, `outbounds`** modules feature completely decoupled `commonMain` code, agnostic to the host system. Platform dependencies (like IO, threading semantics, and logging wrappers) are shunted into platform-specific domains (`jvmMain`, `jsMain`).
- **HTTP Transport Execution:** The common contract uses `HttpTransportClient`. In `jvmMain`, it binds to native Ktor engines (like Netty), whereas `jsMain` binds into browser/Node targets.
- **Bindings & KSP Serialization:** Code generation occurs on compilation `build/generated/ksp/` rendering specific metadata binders that dynamically read/write Ktor-compatible metadata blocks.

### **Package Topology**
1. **`contracts`**: External representations of LLM REST structures (Anthropic, Gemini, OpenAI interfaces).
2. **`core`**: Contains the pipeline architecture, domain definitions, and cross-cutting functional constructs (`Either`, `TypedMap`).
3. **`inbound`/`outbound`**: Symmetrical edge-layers containing provider-specific HTTP translation adapters.
4. **`interceptors`**: The functional middleware blocks.
5. **`mcp-broker`**: Implements the Model Context Protocol defining generic mappings for `.tools`, `.resources`, and `.prompts`.

---

## 3. Adapter Implementation (LLM Providers)

The LLM abstraction uses an aggressive Port and Adapter design mapping through bounded translators.

### **Inbound and Outbound Ports**
The system's logic flow starts at the `InboundAdapter` mapped dynamically in the API routing engine. The request travels the `GatewayPipeline` toward a designated `OutboundPort`. 

```kotlin
interface OutboundPort {
    val provider: Provider
    val key: String
    val model: Model
    suspend fun generate(request: CommonRequest): Either<DomainError, CommonResponse>
    suspend fun generateStream(request: CommonRequest): Either<DomainError, Flow<CommonResponseEvent>>
}
```

### **Outbound Execution & Streaming Context**
In `OpenAiOutboundAdapter.kt`, requests traverse two layers dynamically mapping from standard HTTP execution payloads:
1. Translates `CommonRequest` to `OpenAiChatCompletionsRequest` handling prompt injection, limits, and system roles.
2. Sets explicit headers and POST contexts natively via `transportClient`.
3. Handles streaming chunks via `HttpCallResult` parsing, dispatching network or chunking states to Domain Event streams, allowing upstream endpoints to transparently reconnect without breaking HTTP contexts.

### **Model Context Protocol (MCP)**
The `McpServer` provides externalized dynamic capability handling via defined transports (Stdio, SSE, WebSocket).
```kotlin
class McpServer( ... tools: List<McpTool<*>>, resources: List<McpResource>, prompts: List<McpPrompt> ...)
```
By mapping incoming tool signatures statically over the transport configurations via `DomainMapper.mapTool()`, the SDK exposes complex tools globally.

---

## 4. Resilience & Interceptor Pipeline

Every API call inside the `DispatcherPort` enters the `GatewayPipelineChain`, a recursive Interceptor pattern processing middleware asynchronously.

### **Pipeline Execution Logic**
The pipeline explicitly enforces two modes (`executeUnary` and `executeStream`) executing the same raw inter-intercept context `GatewayContext` mapped recursively downwards. 

### **Resilience & CircuitBreaking Pattern**
Defined in `CircuitBreakerInterceptor.kt`, dynamic fallback evaluation depends on `AttributeKey` mapping stored in pipeline attributes. 
- Fast fail implementation records runtime errors via a distributed (or in-memory) `CircuitBreakerStore`.
- The interceptor proactively injects failure configurations downstream by mutating `deniedOutboundsKey` if a state is `OPEN`. 
- State transitions track discrete requests, transitioning to `HALF_OPEN` silently validating probes.

---

## 5. Security, Auth & Telemetry

Authentication executes entirely prior to execution context mapping via OIDC/OAuth2 protocols and custom Auth Interceptors mapped dynamically globally.

### **Authentication Implementation**
Mapped in the `auth/domain` subsystem. The system extracts structured parameters wrapped inside `AuthValidationResult`.
```kotlin
sealed interface AuthValidationResult {
    data class Jwt(val decoded: DecodedJwt) : AuthValidationResult
    data class Opaque(val introspectionResult: IntrospectionResult) : AuthValidationResult
    data class PassThrough(val token: AuthToken) : AuthValidationResult
}
```

### **Metrics & Tracing**
Defined natively in `Main.kt` alongside `OmniAiGateway`. A configured `TelemetryRuntimeFactory` injects tracking via standard OpenTelemetry metrics binding:
- `gateway.llm.tokens`: Maps to `result.response.usage`.
- `gateway.requests.errors`: Captures pipeline failures per type.
- Extracts telemetry attributes automatically binding Auth identifiers (`user.email`, `user.username`, `discovery`, `aud`) securely mapped from private claims directly generated by the Auth Validation Interceptor.

---

## 6. Crucial Code Snippets

### **A. Interceptor Pipeline Execution Flow (`GatewayPipelineChain.kt`)**
*Demonstrates the core recursive middleware dispatcher wrapping responses via Unary and Stream boundaries.*
```kotlin
internal class GatewayPipelineChain(
    private val interceptors: List<Interceptor>,
    private val dispatcher: DispatcherPort,
    private val index: Int = 0
) : InterceptorChain {
    override suspend fun proceed(context: GatewayContext): PipelineResult {
        if (index >= interceptors.size) {
            return when (context.res) {
                is PipelineResult.Unary -> when (val result = dispatcher.generate(context.request,context.attributes)) {
                    is Either.Right -> PipelineResult.Unary(result.value)
                    is Either.Left -> PipelineResult.Error(result.value)
                }
                is PipelineResult.Stream -> when (val result = dispatcher.generateStream(context.request, context.attributes)) {
                    is Either.Right -> PipelineResult.Stream(result.value)
                    is Either.Left -> PipelineResult.Error(result.value)
                }
                is PipelineResult.Error -> context.res
                is PipelineResult.NoResult -> when (context.mode) {
                    RequestMode.UNARY -> when (val result = dispatcher.generate(context.request, context.attributes)) {
                        is Either.Right -> PipelineResult.Unary(result.value)
                        is Either.Left -> PipelineResult.Error(result.value)
                    }
                    RequestMode.STREAM -> when (val result = dispatcher.generateStream(context.request, context.attributes)) {
                        is Either.Right -> PipelineResult.Stream(result.value)
                        is Either.Left -> PipelineResult.Error(result.value)
                    }
                }
            }
        }
        val nextChain = GatewayPipelineChain(interceptors, dispatcher, index + 1)
        return interceptors[index].handle(context, nextChain)
    }
}
```

### **B. Resilience: Circuit Breaker Interceptor (`CircuitBreakerInterceptor.kt`)**
*Handles strict fallback enforcement without destroying the request lifecycle.*
```kotlin
class CircuitBreakerInterceptor(
    private val store: CircuitBreakerStore,
    private val config: CircuitBreakerConfig,
    private val deniedOutboundsKey: AttributeKey<Set<String>>,
    private val outbounds: List<OutboundPort>
) : Interceptor {
    override suspend fun handle(context: GatewayContext, chain: InterceptorChain): PipelineResult {
        val targetOutbound = outbounds.find { it.provider.value == context.request.provider.value && it.model.model == context.request.model }
            ?: return chain.proceed(context) 

        val outboundId = targetOutbound.key
        val currentState = store.getState(outboundId)

        if (currentState == CircuitState.OPEN) {
            val attributes = context.attributes
            val denied = (attributes[deniedOutboundsKey] ?: emptySet()).toMutableSet()
            denied.add(outboundId)
            attributes[deniedOutboundsKey] = denied

            val error = ApiDownError("Circuit breaker is OPEN for outbound: $outboundId")
            return PipelineResult.Error(error)
        }

        val result = chain.proceed(context)

        when (result) {
            is PipelineResult.Error -> {
                store.recordFailure(outboundId)
                if (store.getFailures(outboundId) >= config.failureThreshold && currentState != CircuitState.OPEN) {
                    store.transitionState(outboundId, CircuitState.OPEN)
                }
            }
            is PipelineResult.Unary, is PipelineResult.Stream -> {
                store.recordSuccess(outboundId)
                if (currentState == CircuitState.HALF_OPEN) {
                    store.transitionState(outboundId, CircuitState.CLOSED)
                }
            }
            is PipelineResult.NoResult -> {}
        }
        return result
    }
}
```

### **C. Adapter Mapping: Flow State Aggregation (`OpenAiOutboundTranslator.kt`)**
*Illustrates advanced data manipulation mapping an OpenAI chunk flow aggressively down into deterministic domain states.*
```kotlin
override fun toDomainEvent(providerEvent: Flow<OpenAiEventStream>): Flow<CommonResponseEvent> =
    providerEvent
        .runningFold(OpenAiEventContext()) { context, event ->
            val translatedEvent = event.toDomainStreamEvent(context.id, context.model)
            context.copy(id = translatedEvent.id, model = translatedEvent.model, event = translatedEvent)
        }
        .mapNotNull { it.event }

private fun OpenAiEventStream.toDomainStreamEvent(previousId: String, previousModel: Model): CommonResponseEvent =
    when (this) {
        is OpenAiEventStream.Chunk -> data.toDomainChunkEvent(previousId, previousModel)
        OpenAiEventStream.Done -> ResponseCompleted(
            provider = Provider.OPENAI, id = previousId, model = previousModel, sequence = 0L, providerEventType = "done"
        )
        is OpenAiEventStream.Error -> ResponseErrored(
            provider = Provider.OPENAI, id = previousId, model = previousModel, sequence = 0L, message = error.message,
            retryable = error.type.isRetryableOpenAiError(), providerEventType = "error"
        )
    }
```

### **D. Ktor Gateway API Configuration & Security DSL (`Main.kt`)**
*Exposes Ktor HTTP bindings with dynamically loaded configurations, authentications, and outbound dispatchers.*
```kotlin
val gateway = omniAiGateway {
    execution {
        useNativePipeline {
            outbounds {
                config.providers.forEach { providerConfig ->
                    when (providerConfig.provider) {
                        ProviderKind.OPENAI -> openAI(httpClient) {
                            baseUrl(providerConfig.baseUrl)
                            apiKey(providerConfig.apiKey) { models(*providerConfig.models.toTypedArray()) }
                        }
                        // Similar implementation dynamically instantiates Gemini/Anthropic 
                    }
                }
            }
            interceptors {
                if (config.telemetryEnabled) {
                    metrics {
                        metricsPort = telemetryRuntime.metricsPort
                        tracer = telemetryRuntime.tracer
                        attributes {
                            attribute("user.email") { gContext, _ ->
                                val authResult = gContext.attributes[AUTH_RESULT_KEY]
                                (authResult as? AuthValidationResult.Jwt)?.decoded?.payload?.privateClaims?.get("email")?.jsonPrimitive?.contentOrNull
                            }
                        }
                    }
                }
            }
        }
    }
    security {
        authentication {
            when (val auth = config.authConfig) {
                is AuthorizationServerGatewayConfig.Oidc -> discovery {
                    discoveryUrl = auth.discoveryUrl
                    expectedAudience = auth.audience
                    clientId = auth.clientId
                    clientSecret = auth.clientSecret
                }
            }
        }
    }
}
```

### **E. Model Context Protocol Server Orchestration (`McpServer.kt`)**
*Displays how tools and runtime features translate to standard contexts under SDK specifications.*
```kotlin
class McpServer( ... ) {
    suspend fun start() {
        val server = Server(
            Implementation(name, version),
            ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(),
                    resources = ServerCapabilities.Resources(subscribe = false, listChanged = false),
                    prompts = ServerCapabilities.Prompts(listChanged = false)
                )
            )
        )

        tools.forEach { tool ->
            val mcpTool = DomainMapper.mapTool(tool)
            // server.addTool(...) dynamically loaded via Kotlin Server Context.
        }

        val transport = when (transportConfig) {
            is StdioTransportConfig -> {
                StdioServerTransport(stdioInput?: error("No Input"), stdioOutput ?: error("No output"))
            }
            // SseTransportConfig and WebSocketTransportConfig stubs available
        }
        server.createSession(transport)
    }
}
```
