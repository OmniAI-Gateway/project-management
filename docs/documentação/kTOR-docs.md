# gateway-ktor-server

Ktor Server transport adapter for exposing the OmniAI Gateway HTTP API.

## Install directly on Ktor Routing

```kotlin
import io.ktor.server.routing.routing
import org.omniai.sdk.gateway.ktor.installAiGateway

routing {
    installAiGateway(runtime) {
        pathPrefix = "/api"
        installOpenAi = true
        installAnthropic = true
        installGemini = true
    }
}
```

## Plug it through `gateway-client` DSL

```kotlin
import io.ktor.server.routing.routing
import org.omniai.sdk.gateway.client.gatewayConfig
import org.omniai.sdk.gateway.ktor.installKtorNetwork

val definition = gatewayConfig {
    installKtorNetwork(routing) {
        pathPrefix = "/api"
    }
}
```

## Metadata binding contract

The adapter creates an `IncomingContext` per request and always executes `ConfigurableMetadataBinder.bind(context)`.

Default binder mappings:
- `Authorization` bearer token -> `gateway.auth.bearerToken`
- request client IP (`X-Forwarded-For` -> `X-Real-IP` -> remote host) -> `gateway.request.clientIp`
- `{model}` path param (Gemini route) -> `gemini.model`

