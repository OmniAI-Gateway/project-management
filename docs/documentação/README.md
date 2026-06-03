# OmniAiGateway

Simple Ktor application that consumes `OmniAi-SDK` using the new Gateway DSL (`gateway-client`) and HTTP adapter (`gateway-ktor-server`).

## Structure

- `src/main/kotlin`: single bootstrap app (`Main.kt`)
- `src/main/resources/application.conf`: default gateway config
- SDK DSL config (`gatewayConfig { ... }`) for outbounds/services/auth
- Ktor route installation via `installAiGateway(runtime)`

Environment variables override values from `application.conf`.

## Environment variables

- `PORT` (default `1900`)
- `OPENAI_API_KEY` (required)
- `OPENAI_MODEL` (default `llama-3.3-70b-versatile`)
- `OPENAI_BASE_URL` (default `https://api.groq.com/openai/v1`)
- `GEMINI_API_KEY` (required)
- `GEMINI_MODEL` (default `gemini-2.5-flash:generateContent`)
- `GEMINI_BASE_URL` (default `https://generativelanguage.googleapis.com/v1beta/models/{model}`)
- `ANTHROPIC_API_KEY` (optional)
- `ANTHROPIC_MODEL` (default `claude-3-7-sonnet-latest`)
- `ANTHROPIC_BASE_URL` (default `https://api.anthropic.com/v1`)

## Endpoints

- `POST /v1/messages` (Anthropic)
- `POST /v1/chat/completions` (OpenAI)
- `POST /v1beta/models/{model}:generateContent` (Gemini)

## Run

```powershell
cd C:\ISEL\TerceiroAno\ProjetoFinal\projectManagement\project-management\OmniAiGateway
.\gradlew run
```


