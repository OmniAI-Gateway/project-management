# gateway-client

Kotlin DSL facade for assembling the OmniAI Gateway runtime.

## Main DSL entrypoint

```kotlin
import org.omniai.sdk.gateway.client.gatewayConfig

val definition = gatewayConfig {
    outbounds {
        // +buildOutbound(OpenAiOutboundAdapter::class, model = "...", apiKey = "...", baseUrl = "...")
        // +buildOutbound(GeminiOutboundAdapter::class, model = "...", apiKey = "...", baseUrl = "...")
        // +buildOutbound(AnthropicOutboundAdapter::class, model = "...", apiKey = "...", baseUrl = "...")
    }

    inbounds {
        openAi = true
        anthropic = true
        gemini = true
        custom("my-provider") { service ->
            MyCustomInbound(service)
        }
    }

    interceptors {
        global(MyGlobalInterceptor())
        local(org.omniai.sdk.domain.common.Provider.OPENAI, MyOpenAiOnlyInterceptor())
        telemetryMetrics {
            meter = myTelemetryMeter
            tracer = myTelemetryTracer // optional
            tags(myTenantKey, myRequestIdKey)
        }
    }

    metrics {
        enable(GatewayMetric.REQUEST_COUNT)
        enable(GatewayMetric.LATENCY)
        telemetry {
            meter = myTelemetryMeter
            tracer = myTelemetryTracer // optional
            tags(myTenantKey, myRequestIdKey)
        }
    }

    services {
        builtIn() // or custom(myInferenceService)
    }

    authorizationServer {
        discovery {
            discoveryUrl = "https://auth.example.com/.well-known/openid-configuration"
            expectedAudience = "gateway-api"
            clientId = "client-id"
            clientSecret = "client-secret"
        }
        // none()
    }
}
```

## KClass-based outbound factory (JVM)

```kotlin
import org.omniai.sdk.adapters.openai.OpenAiOutboundAdapter
import org.omniai.sdk.gateway.client.outbound.buildOutbound

val openAi = buildOutbound(
    outboundClass = OpenAiOutboundAdapter::class,
    model = "llama-3.3-70b-versatile",
    apiKey = System.getenv("OPENAI_API_KEY"),
    baseUrl = "https://api.groq.com/openai/v1"
)
```

## Runtime assembly (JVM)

```kotlin
import org.omniai.sdk.core.http.KtorHttpTransportClient
import org.omniai.sdk.gateway.client.start

suspend fun startGateway() {
    val runtime = definition.start(httpClient = KtorHttpTransportClient.default())
    println("Gateway started with inbounds=${runtime.inbounds}")
}
```

## Telemetry helper (global interceptors)

```kotlin
import org.omniai.sdk.gateway.client.telemetryMetricsInterceptorBuild

val telemetryInterceptors = telemetryMetricsInterceptorBuild {
    meter = myTelemetryMeter
    tracer = myTelemetryTracer // optional
    tags(myTenantKey)
}
```

